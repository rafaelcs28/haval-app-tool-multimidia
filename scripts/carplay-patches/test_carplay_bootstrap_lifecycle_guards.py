"""Guard legacy startup against dropping process-pinned CarPlay callbacks.

The probe cases execute the shell command extracted from the Kotlin source,
with only pidof and /proc paths redirected to local fixtures. No Android build
or device operation is performed.
"""

from pathlib import Path
import re
import shlex
import subprocess
import tempfile
import textwrap
import unittest


ROOT = Path(__file__).resolve().parents[2]
MANAGER = ROOT / "app/src/main/java/br/com/redesurftank/havalshisuku/managers/CarPlayPatchManager.kt"
FRIDA = ROOT / "app/src/main/java/br/com/redesurftank/havalshisuku/utils/FridaUtils.java"


def function_source(source, name):
    start = source.index("    private fun " + name + "(")
    following = re.search(r"\n    (?:private |internal )?fun ", source[start + 1:])
    end = start + 1 + following.start() if following else len(source)
    return source[start:end]


class LegacyBootstrapLifecycleGuardTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manager = MANAGER.read_text()
        cls.frida = FRIDA.read_text()
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
        for signal in ("TERM", "KILL"):
            self.assertRegex(body, r"if \(!clientRefreshAllowed\(reason, verifier\)\) return\s+"
                             r'sh\("kill -' + signal)

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
