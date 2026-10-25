package br.com.redesurftank.havalshisuku.managers

import android.app.Application
import android.content.Context
import android.os.Process
import android.os.SystemClock
import android.util.Log
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.BuildConfig
import br.com.redesurftank.havalshisuku.R
import br.com.redesurftank.havalshisuku.experimental.CarPlayBootstrapRoot
import br.com.redesurftank.havalshisuku.experimental.CarPlayResidentProof
import br.com.redesurftank.havalshisuku.experimental.CarPlaySafetyPublicationGate
import br.com.redesurftank.havalshisuku.experimental.CarPlayBridgeBootstrapPolicy as Policy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Owns local preparation, never the lifetime of published OEM callbacks. */
object CarPlayBridgeBootstrapManager {
    private const val TAG = "CarPlayBridgeBoot"
    private const val ENABLED = "carplayBridgeAutomaticEnabled"
    private const val INPUT = "/data/local/tmp/impulse-carplay-input"
    private const val GATE = "persist.haval.carplay.experimental_cold_hook"
    private const val SERVICE_MD5 = "b61f0e4d63535a2ef447be559211697b"
    private const val ROOT = "/data/local/tmp/impulse-carplay"
    data class Status(val enabled: Boolean, val state: String, val message: String)
    private val mutableStatus = MutableStateFlow(Status(false, "disabled", "Preparo automático desativado"))
    val status: StateFlow<Status> = mutableStatus
    private val started = AtomicBoolean()
    private val inFlight = AtomicBoolean()
    private val safetyWriteLock = Any()
    private val publicationGate = CarPlaySafetyPublicationGate()
    private val worker = Executors.newSingleThreadScheduledExecutor { Thread(it, "carplay-local-bootstrap") }
    private val publisher = Executors.newSingleThreadScheduledExecutor { Thread(it, "carplay-bootstrap-safety") }
    private val root = CarPlayBootstrapRoot()
    @Volatile private var context: Context? = null
    private data class ActiveAttempt(val identity: Identity, val ticket: String) {
        fun publicationKey() = CarPlaySafetyPublicationGate.Key(identity.boot, identity.pid, identity.start, ticket)
    }
    @Volatile private var activeAttempt: ActiveAttempt? = null
    @Volatile private var safety = Safety()
    private var payload: Payload? = null
    private var installedRevision: String? = null
    private var verifiedServiceBoot: String? = null
    private var nextReadyCheckAt = 0L

    private data class Identity(val boot: String, val pid: Int, val start: Long) {
        val key get() = "$boot-$pid-$start"
        val directory get() = "$ROOT/runtime/$key"
    }
    private data class Safety(val wall: Long = 0, val elapsed: Long = 0,
        val quiescent: Boolean? = null)
    private data class Payload(val revision: String, val directory: String,
        val manifest: JSONObject, val source: File) {
        val bridge get() = "$directory/libcarplay_video_bridge.so"
        val injector get() = "$directory/cpbridge-inject"
    }
    private data class Observation(val id: Identity?, val cold: Boolean?, val mappedPath: String?,
        val native: JSONObject, val endpointArmed: Boolean, val nativeOwner: Int, val javaOwner: Int,
        val cpVisual: Boolean, val aaVisual: Boolean,
        val nativeProof: CarPlayResidentProof.Sample? = null,
        val javaProof: CarPlayResidentProof.Sample? = null)

    private fun prefs() = requireNotNull(context).getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
    @JvmStatic fun isAutomaticEnabled(): Boolean = runCatching {
        (context ?: App.getDeviceProtectedContext()).getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
            .getBoolean(ENABLED, false)
    }.getOrDefault(false)

    @JvmStatic fun start(base: Context) {
        if (Application.getProcessName() != base.packageName || !BuildConfig.EMBED_FRIDA_TOOLS || BuildConfig.SIMULATOR_MODE) return
        context = base.createDeviceProtectedStorageContext()
        if (!started.compareAndSet(false, true)) return
        publisher.scheduleWithFixedDelay({ runCatching { publishSafety() } }, 0, 500, TimeUnit.MILLISECONDS)
        worker.scheduleWithFixedDelay({
            try { tick() } catch (error: Exception) {
                Log.w(TAG, "Local preparation deferred", error)
                report("waiting", "Aguardando os serviços locais da central")
            }
        }, 0, 3, TimeUnit.SECONDS)
    }

    @JvmStatic fun setAutomaticEnabled(base: Context, enabled: Boolean) {
        context = base.createDeviceProtectedStorageContext()
        prefs().edit().putBoolean(ENABLED, enabled).commit()
        start(base)
        if (enabled && !inFlight.get()) {
            safety = Safety()
            report("waiting", "Aguardando os serviços locais da central")
            worker.execute { nextReadyCheckAt = 0 }
        }
        if (!enabled) {
            // Revoke the file consumed by native code immediately, even if the worker is busy.
            runCatching { writeSafetyPacket(JSONObject().put("eligible", false)) }
            worker.execute {
            runCatching { root.run("setprop $GATE ''") }
            report("disabled", "Preparo automático desativado; a sessão atual é preservada")
            }
        }
    }

    @JvmStatic fun resetFailureCircuit(base: Context) {
        context = base.createDeviceProtectedStorageContext()
        start(base) // published/unknown mappings still cannot be replaced by a reset
        worker.execute {
            try {
            val bundle = payload ?: extractPayload().also { payload = it }
            val state = observe(bundle)
            val identity = requireNotNull(state.id)
            check(state.mappedPath == null && state.nativeOwner == 0 && state.javaOwner == 0)
            root.run(identityGuard(identity) + script("""
                test -r /proc/${identity.pid}/maps || exit 1
                grep -q libcarplay_video_bridge.so /proc/${identity.pid}/maps
                test §? = 1 || exit 1
                if [ -d ${q(attemptMarker(bundle, identity.boot))} ]; then rmdir ${q(attemptMarker(bundle, identity.boot))} || exit 1; fi
            """))
            check(prefs().edit().remove("cpBridgeCircuitRevision").remove("cpBridgeAttemptBoot")
                .remove("cpBridgeAttemptPhase").commit())
            nextReadyCheckAt = 0
            safety = Safety()
            report("waiting", "Aguardando telefone desconectado e serviços de projeção livres")
            } catch (error: Exception) {
                Log.w(TAG, "Failure reset deferred; current session is preserved", error)
                report("blocked", "A sessão atual deve terminar antes de repetir o preparo")
            }
        }
    }

    private fun report(state: String, message: String) {
        mutableStatus.value = Status(isAutomaticEnabled(), state, message)
    }

    private fun publishSafety() {
        if (!isAutomaticEnabled() && !inFlight.get()) return
        if (!inFlight.get() && mutableStatus.value.state in setOf("ready", "blocked", "failed")) return
        val beginWall = System.currentTimeMillis()
        val beginElapsed = SystemClock.elapsedRealtime()
        val quiet = CarPlayBridgeQuiescence.read(requireNotNull(context))
        safety = Safety(beginWall, beginElapsed, quiet)
        val attempt = activeAttempt
        val identity = attempt?.identity
        val eligible = isAutomaticEnabled() && inFlight.get() && identity != null && quiet == true &&
            System.currentTimeMillis() - beginWall in 0..2500 &&
            SystemClock.elapsedRealtime() - beginElapsed in 0..2500
        val packet = JSONObject().put("bootId", identity?.boot ?: "")
            .put("wallMs", beginWall).put("elapsedMs", beginElapsed)
            .put("targetPid", identity?.pid ?: 0).put("targetStartTime", identity?.start ?: 0)
            .put("ticket", attempt?.ticket ?: "").put("eligible", eligible)
            .put("quiescent", quiet ?: JSONObject.NULL)
            .put("preparationState", mutableStatus.value.state)
        synchronized(safetyWriteLock) {
            val enabled = isAutomaticEnabled()
            val phase = prefs().getString("cpBridgeAttemptPhase", "").orEmpty()
            val decision = publicationGate.decide(attempt?.publicationKey(), activeAttempt?.publicationKey(),
                eligible, enabled, inFlight.get(), phase)
            if (decision == CarPlaySafetyPublicationGate.Decision.DROP) return@synchronized
            packet.put("eligible", decision == CarPlaySafetyPublicationGate.Decision.ALLOW)
                .put("enabled", enabled).put("inFlight", inFlight.get()).put("attemptPhase", phase)
            writeSafetyPacketLocked(packet)
        }
    }

    private fun writeSafetyPacket(packet: JSONObject) = synchronized(safetyWriteLock) {
        publicationGate.cancel(activeAttempt?.ticket ?: prefs().getString("cpBridgeAttemptTicket", ""))
        writeSafetyPacketLocked(packet.put("eligible", false))
    }

    private fun writeSafetyPacketLocked(packet: JSONObject) {
        val temporary = File(INPUT, "safety.next")
        temporary.writeText(packet.toString())
        check(temporary.setReadable(true, false))
        check(temporary.renameTo(File(INPUT, "safety.json")))
    }

    private fun tick() {
        if (!isAutomaticEnabled()) {
            report("disabled", "Preparo automático desativado")
            return
        }
        if (SystemClock.elapsedRealtime() < nextReadyCheckAt) return
        val bundle = payload ?: extractPayload().also { payload = it }
        installPayload(bundle)
        val observed = observe(bundle)
        val id = observed.id
        if (id != null && prefs().getString("cpBridgeAttemptBoot", "").let { it != null && it.isNotEmpty() && it != id.boot } &&
            prefs().getString("cpBridgeAttemptRevision", "") == bundle.revision &&
            prefs().getString("cpBridgeAttemptPhase", "") != "ready") {
            prefs().edit().putString("cpBridgeCircuitRevision", bundle.revision).commit()
        }
        val readyBefore = id != null && prefs().getString("cpBridgeReadyIdentity", "") == id.key &&
            prefs().getString("cpBridgeReadyRevision", "") == bundle.revision
        val phase = when {
            observed.native.optString("phase") == "failed-pinned" -> Policy.Phase.FAILED_PINNED
            observed.native.optString("phase") == "installed-pinned" &&
                (observed.endpointArmed || readyBefore) -> Policy.Phase.READY
            observed.native.optString("phase") == "installed-pinned" -> Policy.Phase.INSTALLED
            observed.native.optString("phase") == "armed" -> Policy.Phase.ARMED
            observed.native.optString("phase") == "failed" -> Policy.Phase.FAILED
            observed.native.optString("phase") in listOf("", "none") -> Policy.Phase.NONE
            else -> Policy.Phase.UNKNOWN
        }
        val current = safety
        val bootAttempt = id != null && prefs().getString("cpBridgeAttemptBoot", "") == id.boot
        val deadline = prefs().getLong("cpBridgeAttemptDeadlineElapsed", 0)
        val recovering = id != null && prefs().getString("cpBridgeAttemptPhase", "") == "preparing" &&
            prefs().getString("cpBridgeAttemptIdentity", "") == id.key &&
            prefs().getString("cpBridgeAttemptRevision", "") == bundle.revision &&
            deadline - SystemClock.elapsedRealtime() in 1L..330_000L
        val ownsNative = observed.nativeProof != null
        val ownsJava = observed.javaProof?.armed == true
        val sameImage = observed.mappedPath == bundle.bridge
        if (observed.mappedPath == null && id != null && verifiedServiceBoot != id.boot) {
            if (!root.run("md5sum /vendor/app/TsCarPlayService/TsCarPlayService.apk").trim().startsWith(SERVICE_MD5)) {
                report("waiting", "Aguardando a preparação dos componentes da central")
                return
            }
            verifiedServiceBoot = id.boot
        }
        val decision = Policy.evaluate(Policy.Snapshot(
            enabled = true, rootReady = true, assetsVerified = true, bootIdentityKnown = id != null,
            processPresent = id != null, processIdentityMatches = id != null &&
                (observed.mappedPath == null || sameImage), phase = phase,
            ownersAlive = ownsNative && (phase != Policy.Phase.READY || ownsJava),
            hasBridgeMapping = observed.mappedPath != null, coldModulesAbsent = observed.cold,
            observationAgeMs = if (current.elapsed > 0) SystemClock.elapsedRealtime() - current.elapsed else null,
            carPlayActive = if (observed.cpVisual) true else current.quiescent?.not(),
            androidAutoActive = if (observed.aaVisual) true else current.quiescent?.not(),
            attemptsThisBoot = if (bootAttempt) 1 else 0,
            failureCircuitOpen = prefs().getString("cpBridgeCircuitRevision", "") == bundle.revision,
            revisionMatches = sameImage || observed.mappedPath == null,
            attemptInProgress = recovering
        ))
        when (decision.action) {
            Policy.Action.PRESERVE_READY -> {
                check(id != null)
                prefs().edit().putString("cpBridgeReadyIdentity", id.key)
                    .putString("cpBridgeReadyRevision", bundle.revision)
                    .putString("cpBridgeAttemptPhase", "ready").commit()
                report("ready", "Bridge preparada para esta sessão")
                nextReadyCheckAt = SystemClock.elapsedRealtime() + 30_000
                refreshClientsOnce(bundle, id)
            }
            Policy.Action.PRESERVE_PINNED -> {
                try { recoverAttempt(bundle, observed) }
                catch (error: Exception) { failAttempt(bundle, error) }
            }
            Policy.Action.COLD_ATTACH, Policy.Action.COLD_RESTART -> attempt(bundle, observed, decision.action)
            Policy.Action.BLOCKED -> {
                if (observed.mappedPath == null && decision.reason == Policy.Reason.ATTEMPT_BUDGET_EXHAUSTED) {
                    runCatching { root.run("setprop $GATE ''") }
                }
                if (sameImage && decision.reason in listOf(Policy.Reason.PINNED_FAILURE, Policy.Reason.PRIOR_FAILURE,
                        Policy.Reason.OWNERS_NOT_ALIVE, Policy.Reason.ATTEMPT_NOT_IN_PROGRESS)) {
                    prefs().edit().putString("cpBridgeCircuitRevision", bundle.revision).commit()
                    runCatching { root.run("setprop $GATE ''") }
                }
                report("blocked", when {
                    observed.mappedPath != null && !sameImage -> "Sessão anterior preservada; aguardando próximo início"
                    else -> "Preparo pausado após uma falha; sessão atual preservada"
                })
                nextReadyCheckAt = SystemClock.elapsedRealtime() + 30_000
            }
            Policy.Action.WAIT_SAFE -> report("waiting", "Aguardando telefone desconectado e serviços de projeção livres")
            Policy.Action.WAIT_PROCESS -> report("waiting", "Aguardando os serviços do CarPlay")
            else -> report("waiting", "Aguardando os serviços locais da central")
        }
    }

    private fun extractPayload(): Payload {
        val app = requireNotNull(context)
        val metadata = JSONObject(app.assets.open("carplay_bridge/manifest.json").bufferedReader().use { it.readText() })
        check(metadata.getInt("schema") == 1 && metadata.getInt("versionCode") == BuildConfig.VERSION_CODE)
        val revision = metadata.getString("revision")
        check(revision.matches(Regex("[0-9]+-[a-f0-9]{12}-[a-f0-9]{12}")))
        val directory = "$ROOT/versions/$revision"
        check(metadata.getString("remoteDirectory") == directory)
        val source = File(app.filesDir, "carplay-bridge/$revision").apply { mkdirs() }
        val files = metadata.getJSONArray("files")
        for (index in 0 until files.length()) {
            val entry = files.getJSONObject(index)
            val name = entry.getString("name")
            check(name.matches(Regex("[a-zA-Z0-9_.-]+")))
            copyVerified(File(source, name), entry.getString("sha256"), entry.getLong("size")) {
                app.assets.open("carplay_bridge/$name")
            }
        }
        copyVerified(File(source, "cpbridge-inject"), metadata.getString("injectorSha256"), metadata.getLong("injectorSize")) {
            app.resources.openRawResource(R.raw.fridainject)
        }
        return Payload(revision, directory, metadata, source)
    }

    private fun copyVerified(file: File, digest: String, size: Long, input: () -> InputStream) {
        check(digest.matches(Regex("[a-f0-9]{64}")) && size > 0)
        if (file.isFile && file.length() == size && sha(file) == digest) return
        val temporary = File(file.parentFile, file.name + ".next")
        input().use { stream -> temporary.outputStream().use { stream.copyTo(it, 65536) } }
        check(temporary.length() == size && sha(temporary) == digest)
        check(temporary.renameTo(file))
    }

    private fun sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private fun sha(file: File): String {
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream -> val bytes = ByteArray(65536); while (true) {
            val count = stream.read(bytes); if (count < 0) break; hash.update(bytes, 0, count)
        } }
        return hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    private fun installPayload(bundle: Payload) {
        if (installedRevision == bundle.revision) return
        val files = bundle.manifest.getJSONArray("files")
        val entries = (0 until files.length()).map { files.getJSONObject(it) }.toMutableList()
        entries += JSONObject().put("name", "cpbridge-inject")
            .put("sha256", bundle.manifest.getString("injectorSha256"))
        fun verify(directory: String) = entries.joinToString("; ") {
            script("""test "§(sha256sum ${q(directory + "/" + it.getString("name"))} | cut -d' ' -f1)" = ${q(it.getString("sha256"))} || exit 1""")
        }
        val temporary = bundle.directory + ".install-" + UUID.randomUUID().toString().replace("-", "")
        val copies = entries.joinToString("; ") {
            "cp ${q(File(bundle.source, it.getString("name")).absolutePath)} ${q(temporary + "/" + it.getString("name"))} || exit 1"
        }
        val traversalParents = listOf(ROOT, "$ROOT/versions", "$ROOT/runtime").joinToString(" ") { q(it) }
        root.run("mkdir -p $traversalParents || exit 1; " +
            "chown 0:0 $traversalParents || exit 1; " +
            "chmod 711 $traversalParents || exit 1; " +
            "if [ -d ${q(bundle.directory)} ]; then ${verify(bundle.directory)}; else " +
            "mkdir ${q(temporary)} || exit 1; $copies; ${verify(temporary)}; " +
            "chown -R 1000:1000 ${q(temporary)}; chmod 500 ${q(temporary)}; chmod 400 ${q(temporary)}/*; " +
            "chmod 500 ${q("$temporary/cpbridge-inject")}; mv ${q(temporary)} ${q(bundle.directory)} || exit 1; fi; " +
            "mkdir -p ${q(INPUT)}; chown ${Process.myUid()}:1000 ${q(INPUT)}; chmod 750 ${q(INPUT)}", 15_000)
        installedRevision = bundle.revision
    }

    private fun observe(bundle: Payload, minimal: Boolean = false, includeVisuals: Boolean = !minimal): Observation {
        val output = root.run(script("""
            printf 'boot='; cat /proc/sys/kernel/random/boot_id
            p=§(pidof com.ts.carplay)
            printf 'pid=%s\n' "§p"
            if [ -n "§p" ] && [ "§{p#* }" = "§p" ]; then
              printf 'stat='; cat /proc/§p/stat
              test -r /proc/§p/maps || exit 1
              grep -qi -e libairplay_ts -e libcarplaystack -e libcoreutils_ts /proc/§p/maps
              cpb_maps_rc=§?
              printf 'cold='; case §cpb_maps_rc in 0) echo no;; 1) echo yes;; *) exit 1;; esac
              printf 'bridge='; grep 'libcarplay_video_bridge.so' /proc/§p/maps | head -n 1; echo
            fi
            if [ ${if (includeVisuals) "1" else "0"} = 1 ]; then
              cpb_stacks=§(am stack list 2>/dev/null) || exit 1
              printf '%s\n' "§cpb_stacks" | grep -q 'Stack id=' || exit 1
              printf 'cp_visual='; printf '%s\n' "§cpb_stacks" | grep -c 'com.ts.carplay.app' || true
              printf 'aa_visual='; printf '%s\n' "§cpb_stacks" | grep -e 'com.google.android.projection.gearhead' -e 'com.ts.androidauto' -c || true
            fi
            true
        """))
        val values = fields(output)
        val boot = values["boot"]?.takeIf { it.matches(Regex("[a-f0-9-]{36}")) }
        val pid = values["pid"]?.toIntOrNull()
        val start = startTime(values["stat"].orEmpty(), pid ?: -1)
        val id = if (boot != null && pid != null && start != null) Identity(boot, pid, start) else null
        val mapped = values["bridge"]?.trim()?.takeIf(String::isNotEmpty)?.let {
            if (it.contains("(deleted)")) "deleted" else it.split(Regex("\\s+")).getOrNull(5)
        }
        if (id == null) return Observation(null, null, mapped, JSONObject(), false, 0, 0,
            values["cp_visual"] != "0", values["aa_visual"] != "0")
        val run = id.directory
        val details = root.run(script("""
            printf 'native='; if [ -f ${q("$run/cold-state.json")} ]; then cat ${q("$run/cold-state.json")}; else echo '{}'; fi
            if [ ${if (minimal) "0" else "1"} = 1 ]; then
            echo 'endpoint_begin'
            tail -n 40 ${q("$run/endpoint-state.log")} 2>/dev/null || true
            echo 'endpoint_end'
            for c in §(pidof cpbridge-inject); do
              a=§(tr '\000' ' ' < /proc/§c/cmdline)
              a=§{a% }
              role=
              case "§a" in
                ${q("${bundle.injector} -p ${id.pid} -s $run/cold-launcher.js -R qjs")}) role=native_owner;;
                ${q("${bundle.injector} -p ${id.pid} -s ${bundle.directory}/endpoint.js -R qjs")}) role=java_owner;;
              esac
              if [ -n "§role" ]; then printf '%s=%s\n' "§role" "§c"; printf '%s_stat=' "§role"; cat /proc/§c/stat; fi
            done
            fi
            printf 'stat_after='; cat /proc/${id.pid}/stat
            printf 'boot_after='; cat /proc/sys/kernel/random/boot_id
        """))
        val data = fields(details)
        check(startTime(data["stat_after"].orEmpty(), id.pid) == id.start) { "OEM identity changed" }
        check(data["boot_after"] == id.boot) { "Kernel boot changed" }
        fun owner(name: String): Int {
            val candidate = data[name]?.toIntOrNull() ?: return 0
            return if ((startTime(data[name + "_stat"].orEmpty(), candidate) ?: 0) >= id.start) candidate else 0
        }
        val nativeProof = if (!minimal && mapped == bundle.bridge) residentSampleOrNull(bundle, id, "native") else null
        val javaProof = if (!minimal && mapped == bundle.bridge) residentSampleOrNull(bundle, id, "java") else null
        val nativeState = if (nativeProof != null) JSONObject().put("phase", nativeProof.phase)
            .put("replacements", nativeProof.replacements).put("pinned", nativeProof.pinned)
            .put("failure", if (nativeProof.failureAbsent) JSONObject.NULL else "Resident native failure")
            else runCatching { JSONObject(data["native"].orEmpty()) }.getOrDefault(JSONObject().put("phase", "unknown"))
        return Observation(id, values["cold"]?.let { when (it) { "yes" -> true; "no" -> false; else -> null } },
            mapped, nativeState, javaProof?.armed == true,
            owner("native_owner"), owner("java_owner"), values["cp_visual"] != "0", values["aa_visual"] != "0",
            nativeProof, javaProof).also {
                if (nativeProof != null || javaProof != null) root.run(identityGuard(id) + ":")
            }
    }

    private fun attempt(bundle: Payload, before: Observation, action: Policy.Action) {
        val original = before.id ?: return
        val attemptTicket = UUID.randomUUID().toString()
        synchronized(safetyWriteLock) {
            if (!inFlight.compareAndSet(false, true)) return
            activeAttempt = ActiveAttempt(original, attemptTicket)
        }
        try {
            check(isAutomaticEnabled())
            check(prefs().edit().putString("cpBridgeAttemptBoot", original.boot)
                .putString("cpBridgeAttemptRevision", bundle.revision).putString("cpBridgeAttemptPhase", "preparing")
                .putString("cpBridgeAttemptTicket", attemptTicket)
                .putString("cpBridgeAttemptIdentity", original.key)
                .putBoolean("cpBridgeAttemptWarmRestart", action == Policy.Action.COLD_RESTART)
                .putLong("cpBridgeAttemptDeadlineElapsed", SystemClock.elapsedRealtime() + 330_000).commit())
            report("preparing", "Preparando a conexão com o cluster")
            // Mount-only work belongs to the existing patch manager. Never start a
            // cold attempt against a different/unmounted service APK.
            check(root.run("md5sum /vendor/app/TsCarPlayService/TsCarPlayService.apk").trim().startsWith(SERVICE_MD5))
            // A root-owned atomic marker also survives app death and transport timeouts.
            root.run("mkdir -p ${q("$ROOT/attempts")}; chmod 700 ${q("$ROOT/attempts")}; " +
                "mkdir ${q(attemptMarker(bundle, original.boot))} || exit 1; " +
                "chmod 700 ${q(attemptMarker(bundle, original.boot))}")
            val existing = root.run("getprop $GATE").trim()
            val nonce = existing.takeIf { it.matches(Regex("[a-f0-9]{32}")) } ?: UUID.randomUUID().toString().replace("-", "")
            root.run("setprop $GATE $nonce")
            repeat(8) { if (File(INPUT, "safety.json").isFile) return@repeat; Thread.sleep(100) }
            if (action == Policy.Action.COLD_RESTART) {
                requireFreshSafety(original)
                val checked = observe(bundle)
                check(checked.id == original && checked.mappedPath == null && !checked.cpVisual && !checked.aaVisual)
                root.run(script("""
                    test "§(cat /proc/sys/kernel/random/boot_id)" = ${q(original.boot)} || exit 1
                    test "§(pidof com.ts.carplay)" = ${original.pid} || exit 1
                    cpb_stacks=§(am stack list 2>/dev/null) || exit 1
                    printf '%s\n' "§cpb_stacks" | grep -q 'Stack id=' || exit 1
                    if printf '%s\n' "§cpb_stacks" | grep -q -e com.ts.carplay.app -e com.google.android.projection.gearhead -e com.ts.androidauto; then exit 1; fi
                    test -r /proc/${original.pid}/maps || exit 1
                    grep -q libcarplay_video_bridge.so /proc/${original.pid}/maps
                    test §? = 1 || exit 1
                    cpb_stat=§(cat /proc/${original.pid}/stat) || exit 1
                    cpb_tail=§{cpb_stat##*) }; set -- §cpb_tail; shift 19
                    test "§1" = ${original.start} || exit 1
                """) + "\n" + CarPlaySafetyPublicationGate.warmKillCheck(
                    CarPlaySafetyPublicationGate.Key(original.boot, original.pid, original.start, attemptTicket),
                    "$INPUT/safety.json") + "kill -15 ${original.pid}\n")
            }
            val waitUntil = SystemClock.elapsedRealtime() + 5_000
            var current = before
            while (SystemClock.elapsedRealtime() < waitUntil) {
                current = observe(bundle, minimal = true)
                if (current.id != null && (action == Policy.Action.COLD_ATTACH || current.id != original)) break
                Thread.sleep(80)
            }
            val identity = requireNotNull(current.id)
            check(current.cold == true && (action == Policy.Action.COLD_ATTACH || identity != original))
            synchronized(safetyWriteLock) { activeAttempt = ActiveAttempt(identity, attemptTicket) }
            check(prefs().edit().putString("cpBridgeAttemptIdentity", identity.key).commit())
            val run = identity.directory
            val template = File(bundle.source, "cold-launcher-template.js").readText()
            val marker = "\"expectedPid\":1234567890"
            check(template.split(marker).size == 2)
            val exact = template.replace(marker, "\"expectedPid\":${identity.pid}")
                .replace("BOOT_PROCESS_PLACEHOLDER", identity.key)
            val launcher = File(requireNotNull(context).filesDir, "carplay-launcher-${identity.key}.js")
            launcher.writeText(exact)
            root.run(script("""
                mkdir -p ${q(run)}; chown 1000:1000 ${q(run)}; chmod 700 ${q(run)}
                test ! -e ${q("$run/cold-launcher.js")} || exit 1
                cp ${q(launcher.absolutePath)} ${q("$run/cold-launcher.js")} || exit 1
                chown 1000:1000 ${q("$run/cold-launcher.js")}; chmod 400 ${q("$run/cold-launcher.js")}
                test "§(sha256sum ${q("$run/cold-launcher.js")} | cut -d' ' -f1)" = ${q(sha(launcher))}
            """))
            requireFreshSafety(identity)
            launch(bundle, identity, "$run/cold-launcher.js", "$run/native-injector.log")
            val proof = awaitResident(bundle, identity, "native", SystemClock.elapsedRealtime() + 4_000)
            check(CarPlayResidentProof.nativeArmed(proof) || CarPlayResidentProof.nativeInstalled(proof)) {
                "Native resident peer not prepared before gate release"
            }
            // Only a fresh response produced after the exact CLI exited can release the gate.
            root.run(identityGuard(identity) + "setprop $GATE " + UUID.randomUUID().toString().replace("-", ""))
            recoverAttempt(bundle, observe(bundle, minimal = true))
            return
        } catch (error: Exception) {
            failAttempt(bundle, error)
        } finally {
            synchronized(safetyWriteLock) {
                inFlight.set(false)
                activeAttempt = null
            }
        }
    }

    private fun recoverAttempt(bundle: Payload, initial: Observation) {
        val identity = initial.id ?: return
        val adopted = synchronized(safetyWriteLock) { inFlight.compareAndSet(false, true) }
        try {
        val attemptTicket = prefs().getString("cpBridgeAttemptTicket", "").orEmpty()
        check(attemptTicket.matches(Regex("[a-f0-9-]{36}")))
        check(prefs().getString("cpBridgeAttemptIdentity", "") == identity.key)
        synchronized(safetyWriteLock) { activeAttempt = ActiveAttempt(identity, attemptTicket) }
        val deadline = prefs().getLong("cpBridgeAttemptDeadlineElapsed", 0)
        check(deadline - SystemClock.elapsedRealtime() in 1L..330_000L)
        while (SystemClock.elapsedRealtime() < deadline) {
            val state = observe(bundle, includeVisuals = false)
            check(state.id == identity) { "OEM identity changed during resident preparation" }
            check(state.nativeProof != null) { "Native resident proof unavailable during preparation" }
            when (state.native.optString("phase")) {
                "failed", "failed-pinned" -> error("Native preparation failed")
                "installed-pinned" -> {
                    check(CarPlayResidentProof.nativeInstalled(requireNotNull(state.nativeProof))) { "Native resident installation is incomplete" }
                    if (state.endpointArmed && state.javaProof?.armed == true) {
                        check(prefs().edit().putString("cpBridgeReadyIdentity", identity.key)
                            .putString("cpBridgeReadyRevision", bundle.revision)
                            .putString("cpBridgeAttemptPhase", "ready").commit())
                        report("ready", "Bridge preparada para esta sessão")
                        nextReadyCheckAt = SystemClock.elapsedRealtime() + 30_000
                        refreshClientsOnce(bundle, identity)
                        return
                    }
                    if (state.javaProof == null) {
                        check(isAutomaticEnabled())
                        if (!prefs().getBoolean("cpBridgeJavaIssued_${identity.key}", false)) {
                            check(prefs().edit().putBoolean("cpBridgeJavaIssued_${identity.key}", true).commit()) { "Could not persist Java launch admission" }
                            launch(bundle, identity, "${bundle.directory}/endpoint.js", "${identity.directory}/java-injector.log")
                        }
                        awaitResident(bundle, identity, "java", minOf(deadline, SystemClock.elapsedRealtime() + 15_000))
                    }
                }
            }
            Thread.sleep(1_000)
        }
        error("Local preparation timed out")
        } finally {
            if (adopted) synchronized(safetyWriteLock) {
                inFlight.set(false)
                activeAttempt = null
            }
        }
    }

    private fun failAttempt(bundle: Payload, error: Exception) {
        Log.e(TAG, "Automatic preparation stopped; published owners are preserved", error)
        val trace = org.json.JSONArray()
        error.stackTrace.take(12).forEach { trace.put(it.toString()) }
        val failure = JSONObject().put("eligible", false)
            .put("failureType", error.javaClass.simpleName)
            .put("failureMessage", error.message ?: "").put("failureStack", trace)
        synchronized(safetyWriteLock) {
            publicationGate.cancel(activeAttempt?.ticket ?: prefs().getString("cpBridgeAttemptTicket", ""))
            // Keep the failure separate: a late diagnostic sample may replace safety.json.
            prefs().edit().putString("cpBridgeCircuitRevision", bundle.revision)
                .putString("cpBridgeAttemptPhase", "failed")
                .putString("cpBridgeLastFailure", failure.toString()).commit()
            runCatching { writeSafetyPacketLocked(failure) }
        }
        runCatching { root.run("setprop $GATE ''") }
        report("failed", "O preparo foi interrompido; sessão atual preservada")
    }

    private fun refreshClientsOnce(bundle: Payload, identity: Identity) {
        val key = "cpBridgeClientsIssued_${identity.key}"
        if (prefs().getString("cpBridgeAttemptIdentity", "") != identity.key ||
            prefs().getString("cpBridgeAttemptRevision", "") != bundle.revision ||
            !prefs().getBoolean("cpBridgeAttemptWarmRestart", false) || prefs().getBoolean(key, false)) return
        // No retry after an ambiguous transport result or an already connected phone.
        if (!prefs().edit().putBoolean(key, true).commit()) return
        runCatching {
            CarPlayPatchManager.postBootstrapClientRefresh {
                fun safeNow(): Boolean {
                    val sample = safety
                    return isAutomaticEnabled() && sample.quiescent == true && sample.elapsed > 0 &&
                        SystemClock.elapsedRealtime() - sample.elapsed in 0..2500
                }
                if (!safeNow()) false else {
                    val state = observe(bundle)
                    state.id == identity && state.mappedPath == bundle.bridge && state.nativeProof?.let(CarPlayResidentProof::nativeInstalled) == true &&
                        state.javaProof?.armed == true && state.native.optString("phase") == "installed-pinned" &&
                        state.endpointArmed && !state.cpVisual && !state.aaVisual && safeNow()
                }
            }
        }.onFailure { Log.w(TAG, "Native client refresh deferred; bridge owners preserved", it) }
    }

    private fun attemptMarker(bundle: Payload, boot: String) = "$ROOT/attempts/$boot-${bundle.revision}"

    private fun launch(bundle: Payload, identity: Identity, scriptPath: String, log: String) {
        val now = observe(bundle)
        check(now.id == identity)
        check(isAutomaticEnabled())
        if (scriptPath.endsWith("/cold-launcher.js")) {
            check(now.cold == true && now.mappedPath == null && !now.cpVisual && !now.aaVisual)
            requireFreshSafety(identity)
        }
        val role = if (scriptPath.endsWith("/cold-launcher.js")) "native" else "java"
        val binding = residentBinding(bundle, identity, role, UUID.randomUUID().toString())
        check(binding.scriptPath == scriptPath && log == "${identity.directory}/$role-injector.log") { "Unexpected resident launch path" }
        val supervisor = File(requireNotNull(context).filesDir, "carplay-$role-supervisor-${identity.key}.sh")
        supervisor.writeText(CarPlayResidentProof.supervisor(binding))
        val remote = "${identity.directory}/$role-supervisor.sh"
        root.run(identityGuard(identity) + script("""
            test -d ${q(attemptMarker(bundle, identity.boot))} || exit 1
            test ! -e ${q(remote)} || exit 1
            cp ${q(supervisor.absolutePath)} ${q(remote)} || exit 1
            chown 0:0 ${q(remote)}; chmod 500 ${q(remote)}
            nohup setsid sh ${q(remote)} > ${q("${identity.directory}/$role-supervisor.log")} 2>&1 < /dev/null & :
        """))
    }

    private fun nativeLauncher(bundle: Payload, identity: Identity): String {
        val template = File(bundle.source, "cold-launcher-template.js").readText()
        val marker = "\"expectedPid\":1234567890"
        check(template.split(marker).size == 2) { "Cold launcher PID marker mismatch" }
        return template.replace(marker, "\"expectedPid\":${identity.pid}")
            .replace("BOOT_PROCESS_PLACEHOLDER", identity.key)
    }

    private fun residentBinding(bundle: Payload, identity: Identity, role: String, instance: String): CarPlayResidentProof.Binding {
        check(prefs().getString("cpBridgeAttemptIdentity", "") == identity.key &&
            prefs().getString("cpBridgeAttemptRevision", "") == bundle.revision) { "Resident attempt identity mismatch" }
        val ticket = prefs().getString("cpBridgeAttemptTicket", "").orEmpty()
        val scriptHash = if (role == "native") sha(nativeLauncher(bundle, identity).toByteArray(Charsets.UTF_8))
            else sha(File(bundle.source, "endpoint.js"))
        return CarPlayResidentProof.Binding(role, identity.boot, identity.pid, identity.start,
            bundle.revision, ticket, instance, scriptHash, identity.directory, bundle.directory)
            .also(CarPlayResidentProof::checkBinding)
    }

    private fun strictString(value: JSONObject, key: String): String =
        (value.opt(key) as? String) ?: error("Resident field $key must be a string")
    private fun strictLong(value: JSONObject, key: String): Long {
        val number = value.opt(key)
        check(number is Int || number is Long) { "Resident field $key must be an integer" }
        return (number as Number).toLong()
    }
    private fun strictPid(value: JSONObject, key: String): Int = strictLong(value, key).let {
        check(it in 1..Int.MAX_VALUE.toLong()) { "Resident field $key is not a PID" }; it.toInt()
    }
    private fun parseBinding(value: JSONObject): CarPlayResidentProof.Binding {
        check(strictLong(value, "schema") == 1L) { "Unknown resident protocol schema" }
        return CarPlayResidentProof.Binding(strictString(value, "role"), strictString(value, "bootId"),
            strictPid(value, "targetPid"), strictLong(value, "targetStartTime"), strictString(value, "revision"),
            strictString(value, "ticket"), strictString(value, "instance"), strictString(value, "scriptSha256"),
            strictString(value, "runtimeDirectory"), strictString(value, "payloadDirectory")).also(CarPlayResidentProof::checkBinding)
    }
    private fun parseReceipt(value: JSONObject, exited: Boolean) = CarPlayResidentProof.Receipt(
        parseBinding(value), strictPid(value, "childPid"), strictLong(value, "childStartTime"),
        strictString(value, "commandSha256"),
        if (exited) strictLong(value, "exitCode").also { check(it in 0..255) }.toInt() else null,
        if (exited) strictLong(value, "exitElapsedMs") else null)

    private fun parseResidentResponse(value: JSONObject): CarPlayResidentProof.Response {
        val samples = value.getJSONArray("samples")
        check(samples.length() == 2) { "Resident peer must return two samples" }
        return CarPlayResidentProof.Response(parseBinding(value), strictString(value, "nonce"),
            value.opt("disposing") as? Boolean, (0 until samples.length()).map { index ->
                val sample = samples.getJSONObject(index)
                val state = sample.getJSONObject("state")
                CarPlayResidentProof.Sample(strictLong(sample, "sequence"), strictLong(sample, "elapsedMs"),
                    strictLong(sample, "wallMs"), state.optString("phase"),
                    if (state.has("replacements")) strictLong(state, "replacements").also { check(it in 0..9) { "Invalid resident replacement count" } }.toInt() else -1,
                    state.opt("pinned") == true, state.has("failure") && state.isNull("failure"),
                    state.opt("armed") == true)
            })
    }

    private fun residentSampleOrNull(bundle: Payload, identity: Identity, role: String): CarPlayResidentProof.Sample? {
        if (role == "java" && !prefs().getBoolean("cpBridgeJavaIssued_${identity.key}", false)) return null
        return try { residentSample(bundle, identity, role) }
        catch (error: Exception) {
            Log.w(TAG, "$role resident proof unavailable: ${error.message}")
            null
        }
    }

    private fun awaitResident(bundle: Payload, identity: Identity, role: String, deadline: Long): CarPlayResidentProof.Sample {
        var last: Exception? = null
        while (SystemClock.elapsedRealtime() < deadline) {
            try { return residentSample(bundle, identity, role) }
            catch (error: Exception) { last = error; Thread.sleep(80) }
        }
        throw IllegalStateException("$role post-exit resident proof unavailable: ${last?.message}", last)
    }

    private fun residentSample(bundle: Payload, identity: Identity, role: String): CarPlayResidentProof.Sample {
        check(role in setOf("native", "java"))
        val launchPath = "${identity.directory}/$role-launch.json"
        val exitPath = "${identity.directory}/$role-exit.json"
        val receipts = root.run(identityGuard(identity) + script("""
            for cpb_receipt in ${q(launchPath)} ${q(exitPath)}; do
              test -f "§cpb_receipt" || exit 1
              test "§(stat -c %u "§cpb_receipt")" = 0 || exit 1
              test "§(stat -c %a "§cpb_receipt")" = 400 || exit 1
              test "§(wc -c < "§cpb_receipt")" -le 4096 || exit 1
            done
            printf 'launch='; cat ${q(launchPath)}; printf '\nexit='; cat ${q(exitPath)}; echo
        """))
        val values = fields(receipts)
        val launch = parseReceipt(JSONObject(requireNotNull(values["launch"])), false)
        val exited = parseReceipt(JSONObject(requireNotNull(values["exit"])), true)
        val binding = residentBinding(bundle, identity, role, launch.binding.instance)
        CarPlayResidentProof.checkExit(binding, launch, exited)
        // This nonce is created only AFTER validating the exact child's successful exit.
        val challenge = CarPlayResidentProof.Challenge(binding.instance, UUID.randomUUID().toString(), SystemClock.elapsedRealtime())
        val packet = JSONObject().put("schema", 1).put("instance", challenge.instance)
            .put("nonce", challenge.nonce).put("issuedElapsedMs", challenge.issuedElapsedMs).toString()
        val challengePath = "${identity.directory}/$role-residency-challenge.json"
        val responsePath = "${identity.directory}/$role-residency-response.json"
        val responseText = root.run(identityGuard(identity) + script("""
            printf '%s\n' ${q(packet)} > ${q("$challengePath.next")} || exit 1
            chown 1000:1000 ${q("$challengePath.next")} && chmod 400 ${q("$challengePath.next")} || exit 1
            mv ${q("$challengePath.next")} ${q(challengePath)} || exit 1
            cpb_round=0
            cpb_response=
            while [ "§cpb_round" -lt 24 ]; do
              cpb_round=§((cpb_round+1))
              if [ -f ${q(responsePath)} ] && [ "§(wc -c < ${q(responsePath)})" -le 8192 ]; then
                cpb_response=§(cat ${q(responsePath)})
                if printf '%s' "§cpb_response" | grep -Fq ${q("\"nonce\":\"${challenge.nonce}\"")}; then break; fi
              fi
              cpb_response=
              sleep 0.05
            done
            test -n "§cpb_response" || exit 1
        """) + "\n" + identityGuard(identity) + "printf '%s\\n' \"\$cpb_response\"\n")
        val sample = CarPlayResidentProof.checkResponse(binding, launch, exited, challenge,
            parseResidentResponse(JSONObject(responseText)), SystemClock.elapsedRealtime(), System.currentTimeMillis())
        if (role == "native") check(CarPlayResidentProof.nativeArmed(sample) || CarPlayResidentProof.nativeInstalled(sample)) {
            "Native resident phase invalid: ${sample.phase}/${sample.replacements}/${sample.failureAbsent}"
        } else check(sample.armed) { "Java resident peer is not armed" }
        return sample
    }

    private fun identityGuard(identity: Identity) = script("""
        test "§(cat /proc/sys/kernel/random/boot_id)" = ${q(identity.boot)} || exit 1
        test "§(pidof com.ts.carplay)" = ${identity.pid} || exit 1
        cpb_stat=§(cat /proc/${identity.pid}/stat) || exit 1
        cpb_tail=§{cpb_stat##*) }; set -- §cpb_tail
        case "§1" in Z|X|x) exit 1;; esac
        shift 19; test "§1" = ${identity.start} || exit 1
    """) + "\n"

    private fun requireFreshSafety(identity: Identity) {
        val deadline = SystemClock.elapsedRealtime() + 1200
        while (SystemClock.elapsedRealtime() < deadline) {
            val packet = runCatching { JSONObject(File(INPUT, "safety.json").readText()) }.getOrNull()
            val attempt = activeAttempt
            if (isAutomaticEnabled() && inFlight.get() && attempt != null && attempt.identity == identity &&
                packet != null && packet.optBoolean("eligible") && packet.optString("ticket") == attempt.ticket &&
                packet.optString("bootId") == identity.boot && packet.optInt("targetPid") == identity.pid &&
                packet.optLong("targetStartTime") == identity.start &&
                System.currentTimeMillis() - packet.optLong("wallMs") in 0..2500 &&
                SystemClock.elapsedRealtime() - packet.optLong("elapsedMs") in 0..2500) return
            Thread.sleep(50)
        }
        error("Fresh projection quiescence unavailable")
    }

    private fun fields(text: String): Map<String, String> = text.lineSequence().mapNotNull {
        val index = it.indexOf('='); if (index < 1) null else it.substring(0, index) to it.substring(index + 1)
    }.toMap()
    private fun startTime(stat: String, pid: Int): Long? = runCatching {
        if (stat.substringBefore(" (").toInt() != pid) return null
        val tail = stat.substringAfterLast(") ").split(Regex("\\s+"))
        if (tail[0] in listOf("Z", "X", "x")) null else tail[19].toLong().takeIf { it > 0 }
    }.getOrNull()
    private fun q(value: String) = CarPlayBootstrapRoot.quote(value)
    private fun script(value: String) = value.trimIndent().replace('§', '$')
}
