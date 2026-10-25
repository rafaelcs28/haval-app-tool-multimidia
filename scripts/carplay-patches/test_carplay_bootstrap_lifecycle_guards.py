"""Guard legacy startup against dropping process-pinned CarPlay callbacks.

The probe cases execute the shell command extracted from the Kotlin source,
with only pidof and /proc paths redirected to local fixtures. No Android build
or device operation is performed.
"""

from pathlib import Path
import json
import re
import shlex
import subprocess
import tempfile
import textwrap
import unittest


ROOT = Path(__file__).resolve().parents[2]
MANAGER = ROOT / "app/src/main/java/br/com/redesurftank/havalshisuku/managers/CarPlayPatchManager.kt"
FRIDA = ROOT / "app/src/main/java/br/com/redesurftank/havalshisuku/utils/FridaUtils.java"
BOOTSTRAP = ROOT / "app/src/main/java/br/com/redesurftank/havalshisuku/managers/CarPlayBridgeBootstrapManager.kt"
QUIESCENCE = ROOT / "app/src/main/java/br/com/redesurftank/havalshisuku/managers/CarPlayBridgeQuiescence.kt"
BLUETOOTH = ROOT / "app/src/main/java/br/com/redesurftank/havalshisuku/managers/CarPlayBluetoothQuiescence.kt"


def function_source(source, name):
    selected = re.search(r"    private fun (?:<[^>]+>\s+)?" + re.escape(name) + r"\(", source)
    if not selected:
        raise AssertionError("Missing private function: " + name)
    start = selected.start()
    following = re.search(r"\n    (?:private |internal )?fun ", source[start + 1:])
    end = start + 1 + following.start() if following else len(source)
    return source[start:end]


class LegacyBootstrapLifecycleGuardTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manager = MANAGER.read_text()
        cls.frida = FRIDA.read_text()
        cls.bootstrap = BOOTSTRAP.read_text()
        match = re.search(r'private val bridgeMappingProbe = """(.*?)"""\.trimIndent\(\)',
                          cls.manager, re.S)
        if not match:
            raise AssertionError("Live bridge mapping probe is missing")
        cls.probe = textwrap.dedent(match.group(1)).replace("${'$'}", "$").strip()

    def run_probe(self, pids, maps, pidof_status=0, grep_status=None):
        with tempfile.TemporaryDirectory(prefix="impulse-bridge-guard-") as directory:
            fixture = Path(directory)
            pidof = fixture / "fixture-pidof"
            pidof.write_text("#!/bin/sh\nprintf '%s\\n' " + shlex.quote(pids) +
                             "\nexit " + str(pidof_status) + "\n")
            pidof.chmod(0o700)
            for pid, contents in maps.items():
                target = fixture / "proc" / pid / "maps"
                target.parent.mkdir(parents=True)
                target.write_text(contents)
            script = self.probe.replace("pidof com.ts.carplay", shlex.quote(str(pidof)))
            script = script.replace("/proc/", str(fixture / "proc") + "/")
            if grep_status is not None:
                grep = fixture / "fixture-grep"
                grep.write_text("#!/bin/sh\nexit " + str(grep_status) + "\n")
                grep.chmod(0o700)
                script = script.replace("grep -Fq", shlex.quote(str(grep)) + " -Fq")
            result = subprocess.run(["sh", "-c", script], text=True, capture_output=True,
                                    timeout=5, check=True)
            return result.stdout.strip()

    def test_server_start_has_no_global_injector_termination(self):
        self.assertNotRegex(self.frida, r"\b(?:pkill|killall)\b")
        self.assertIn("if (serverExists && injectExists)", self.frida)
        self.assertIn("if (!injectExists)", self.frida)

    def test_automatic_owner_is_checked_before_mapping_probe(self):
        body = function_source(self.manager, "deferPatchProcessReload")
        self.assertLess(body.index("CarPlayBridgeBootstrapManager.isAutomaticEnabled()"),
                        body.index("sh(bridgeMappingProbe)"))
        self.assertIn('"automatic_bootstrap_owner"', body)
        self.assertIn('else -> "bridge_state_unknown"', body)

    def test_reload_defers_before_visual_or_host_actions(self):
        body = function_source(self.manager, "reloadCarPlayProcessesAfterPatchMount")
        self.assertLess(body.index("if (deferPatchProcessReload(reason)) return"),
                        body.index("DisplayAppLauncher.isCarPlayOnDisplay"))

    def test_signals_and_start_fallback_recheck_ownership(self):
        body = function_source(self.manager, "reloadBoundProcessWithoutForceStop")
        for signal in ("TERM", "KILL"):
            with self.subTest(signal=signal):
                self.assertRegex(body, r"if \(deferPatchProcessReload\(reason\)\) return false\s+"
                                 r'sh\("kill -' + signal)
        self.assertRegex(body, r"if \(replacementPids.isEmpty\(\)\) \{\s+"
                         r"if \(deferPatchProcessReload\(reason\)\) return false")

    def test_native_client_refreshes_recheck_ownership(self):
        for name in ("refreshOemCarPlayClientsAfterHostReload", "refreshSystemUiCarPlayView",
                     "refreshAppListCarPlayManager"):
            with self.subTest(name=name):
                body = function_source(self.manager, name)
                self.assertRegex(body, r"\{\s+if \(!clientRefreshAllowed\(reason, verifier\)\) return")

    def test_client_verifier_cannot_bypass_host_reload_guard(self):
        body = function_source(self.manager, "clientRefreshAllowed")
        self.assertIn("if (verifier == null) !deferPatchProcessReload(reason)", body)
        reload = function_source(self.manager, "reloadBoundProcessWithoutForceStop")
        self.assertNotIn("verifier", reload)
        refresh = function_source(self.manager, "refreshSystemUiCarPlayView")
        restore = refresh.split("} finally {", 1)[1].split("val overlayRestored", 1)[0]
        self.assertIn("cmd overlay $restoreAction", restore)
        self.assertNotIn("clientRefreshAllowed", restore)

    def test_client_signals_recheck_verifier_immediately(self):
        body = function_source(self.manager, "refreshAppListCarPlayManager")
        guarded_commands = re.findall(
            r'if \(!clientRefreshAllowed\(reason, verifier\)\) return\s+'
            r'sh\("((?:\\.|[^"\\])*)"\)', body)
        for signal in ("TERM", "KILL"):
            matches = [command for command in guarded_commands if f"kill -{signal} $pid" in command]
            self.assertEqual(len(matches), 1, f"{signal} requires an immediate verifier")
            command = matches[0]
            self.assertTrue(command.startswith(r'[ \"\$(tr '))
            self.assertIn("/proc/$pid/cmdline | head -n 1)", command)
            self.assertIn(r'''\" = '$APP_LIST_PACKAGE' ] && kill -''' + signal + " $pid", command)

    def test_client_signal_shell_rejects_reused_foreign_pid(self):
        body = function_source(self.manager, "refreshAppListCarPlayManager")
        commands = re.findall(r'sh\("((?:\\.|[^"\\])*)"\)', body)
        for signal in ("TERM", "KILL"):
            command = next(command for command in commands if f"kill -{signal} $pid" in command)
            # Kotlin quoted strings use JSON escapes plus \$ for a literal dollar.
            command = json.loads('"' + command.replace(r"\$", "$") + '"')
            for identity, expected in (("com.beantechs.applist", f"-{signal} 1234"),
                                       ("com.ts.carplay", ""), (None, "")):
                with self.subTest(signal=signal, identity=identity), tempfile.TemporaryDirectory() as directory:
                    fixture = Path(directory) / "1234"
                    fixture.mkdir()
                    if identity is not None:
                        (fixture / "cmdline").write_bytes(identity.encode() + b"\0argument\0")
                    script = command.replace("$pid", "1234").replace(
                        "$APP_LIST_PACKAGE", "com.beantechs.applist").replace("/proc/", directory + "/")
                    # Override the shell builtin: the fixture never signals a real process.
                    script = "kill() { printf '%s\\n' \"$*\"; }; " + script
                    result = subprocess.run(["sh", "-c", script], capture_output=True,
                                            text=True, timeout=5, check=True)
                    self.assertEqual(result.stdout.strip(), expected)

    def test_failed_attempt_cannot_be_adopted_as_in_progress(self):
        tick = function_source(self.bootstrap, "tick")
        recovering = tick.split("val recovering =", 1)[1].split("val ownsNative", 1)[0]
        self.assertIn('prefs().getString("cpBridgeAttemptPhase", "") == "preparing" &&', recovering)
        self.assertIn('prefs().getString("cpBridgeAttemptIdentity", "") == id.key &&', recovering)
        self.assertIn('prefs().getString("cpBridgeAttemptRevision", "") == bundle.revision &&', recovering)
        self.assertIn("deadline - SystemClock.elapsedRealtime() in 1L..330_000L", recovering)
        self.assertIn("attemptInProgress = recovering", tick)
        failure = function_source(self.bootstrap, "failAttempt")
        self.assertIn('.putString("cpBridgeAttemptPhase", "failed")', failure)

    def test_exhausted_boot_budget_clears_gate_without_allowing_a_retry(self):
        tick = function_source(self.bootstrap, "tick")
        blocked = tick.split("Policy.Action.BLOCKED -> {", 1)[1]
        match = re.search(
            r'if \(observed\.mappedPath == null && decision\.reason == '
            r'Policy\.Reason\.ATTEMPT_BUDGET_EXHAUSTED\) \{([^}]+)\}', blocked)
        self.assertIsNotNone(match, "Only an unmapped, budget-exhausted process releases the stale gate")
        self.assertIn('root.run("setprop $GATE \'\'")', match.group(1))
        self.assertNotIn('.remove(', match.group(1))
        self.assertNotIn("attempt(", match.group(1))
        self.assertIn("attemptsThisBoot = if (bootAttempt) 1 else 0", tick)

    def test_null_projection_semantics_require_exact_verified_oem_firmware(self):
        source = QUIESCENCE.read_text()
        self.assertIn('DCM_APK_SHA256 = "7859c63e089d6f053df7b4121063e30d4e5f7795cf8fca52a73bebe8a44b9b69"', source)
        self.assertIn("DCM_APK_SIZE = 237_975L", source)
        self.assertLess(source.index("if (!verifyFirmware(context, service.epoch))"),
                        source.index("val online = readOnlineDevicesEmpty"))
        verify = function_source(source, "verifyFirmware")
        self.assertIn("getServiceInfo(ComponentName(DCM_PACKAGE, DCM_SERVICE_CLASS), 0)", verify)
        self.assertIn("File(info.applicationInfo.sourceDir)", verify)
        self.assertIn('MessageDigest.getInstance("SHA-256")', verify)
        self.assertIn("cached.epoch == epoch", verify)
        self.assertIn("cached.modified == modified", verify)
        self.assertRegex(verify, r"catch \(_: Exception\) \{\s+firmwareCheck = null\s+false")

    def test_profiled_null_list_is_absence_but_empty_list_may_be_connecting(self):
        source = QUIESCENCE.read_text()
        decoder = function_source(source, "readActiveProjectionsEmpty")
        self.assertIn("-1 -> return@transactRead true", decoder)
        self.assertIn("0 -> return@transactRead null", decoder)
        self.assertIn("if (count < 0) return@transactRead null", decoder)
        self.assertIn("reply.setDataPosition(listPosition)", decoder)
        self.assertIn("reply.createTypedArrayList(ConnDevice.CREATOR)", decoder)
        self.assertIn("if (devices.isNotEmpty()) false else null", decoder)
        self.assertNotIn("devices.isEmpty()", decoder)
        transaction = function_source(source, "transactRead")
        self.assertIn("if (reply.dataAvail() == 0) result else null", transaction)
        online = function_source(source, "readOnlineDevicesEmpty")
        self.assertIn("if (reply.readInt() != 1) return@transactRead null", online)
        self.assertIn("val entries = keys.map { devices.get(it) }", online)
        self.assertIn("entries.any { it !is ConnDevice }", online)

    def test_retained_records_require_independent_profiled_physical_bluetooth_proof(self):
        source = QUIESCENCE.read_text()
        self.assertIn("CarPlayBluetoothQuiescence.read(context, online.retainedAddresses)", source)
        self.assertIn("activeEmpty != true -> null", source)
        bluetooth = BLUETOOTH.read_text()
        self.assertIn('APK_SHA256 = "c99e49b47704e610c044ab01aa530ca451784e9e78885b11cad1c1f45518eac0"', bluetooth)
        self.assertIn("!verifyFirmware(context, current.epoch)", bluetooth)
        self.assertIn("started - current.at < SETTLE_MS", bluetooth)
        self.assertIn("for (profile in 0..4)", bluetooth)
        self.assertIn("call(binder, 14, 1", bluetooth)
        self.assertIn("if (before != after) return null", bluetooth)
        self.assertIn("val addresses = before + retained", bluetooth)
        self.assertIn("devices(binder, 13, -1)", bluetooth)
        self.assertIn("call(binder, 4, 0", bluetooth)
        self.assertIn("if (reply.readInt() != 1) return null", bluetooth)
        self.assertIn("result == expectedResult && reply.dataAvail() == 0", bluetooth)
        self.assertNotRegex(bluetooth, r"\bLog\.")
        self.assertNotIn("getUsbSerialNumber", source)
        self.assertNotIn("getWifiAddr", source)

    def test_absent_host_is_clear(self):
        self.assertEqual(self.run_probe("", {}, pidof_status=1), "bridge-map-clear")

    def test_uninstrumented_host_keeps_legacy_reload_available(self):
        self.assertEqual(self.run_probe("1234", {
            "1234": "1000-2000 r-xp 00000000 00:00 0 /vendor/libcarplaystack.so\n"
                    "2000-3000 r-xp 00000000 00:00 0 /vendor/libc++_shared.so\n"
        }), "bridge-map-clear")

    def test_bridge_mapping_is_found_even_without_an_automatic_preference(self):
        for suffix in ("", " (deleted)"):
            with self.subTest(suffix=suffix):
                self.assertEqual(self.run_probe("1234", {
                    "1234": "1000-2000 r-xp 00000000 00:00 0 "
                            "/data/local/tmp/impulse-carplay/v132/libcarplay_video_bridge.so" + suffix + "\n"
                }), "bridge-map-present")

    def test_each_host_pid_is_checked(self):
        self.assertEqual(self.run_probe("1234 5678", {
            "1234": "1000-2000 r-xp 00000000 00:00 0 /vendor/libcarplaystack.so\n",
            "5678": "1000-2000 r-xp 00000000 00:00 0 "
                    "/data/local/tmp/impulse-carplay/v133/libcarplay_video_bridge.so\n"
        }), "bridge-map-present")

    def test_disappearing_or_unreadable_maps_do_not_authorize_reload(self):
        self.assertEqual(self.run_probe("1234", {}), "bridge-map-unknown")

    def test_process_probe_errors_do_not_authorize_reload(self):
        self.assertEqual(self.run_probe("", {}, pidof_status=2), "bridge-map-unknown")
        self.assertEqual(self.run_probe("not-a-pid", {}), "bridge-map-unknown")

    def test_grep_errors_are_distinct_from_an_absent_mapping(self):
        self.assertEqual(self.run_probe("1234", {"1234": ""}, grep_status=2),
                         "bridge-map-unknown")


if __name__ == "__main__":
    unittest.main()
