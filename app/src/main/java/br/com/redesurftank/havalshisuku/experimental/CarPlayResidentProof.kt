package br.com.redesurftank.havalshisuku.experimental

import java.security.MessageDigest

/** Pure validation and shell generation. No process restart or callback teardown. */
internal object CarPlayResidentProof {
    data class Binding(val role: String, val bootId: String, val targetPid: Int,
        val targetStartTime: Long, val revision: String, val ticket: String,
        val instance: String, val scriptSha256: String, val runtimeDirectory: String,
        val payloadDirectory: String) {
        val scriptPath get() = if (role == "native") "$runtimeDirectory/cold-launcher.js" else "$payloadDirectory/endpoint.js"
    }
    data class Receipt(val binding: Binding, val childPid: Int, val childStartTime: Long,
        val commandSha256: String, val exitCode: Int? = null, val exitElapsedMs: Long? = null,
        val commandEvidence: String = "", val supervisorSha256: String = "")
    data class Challenge(val instance: String, val nonce: String, val issuedElapsedMs: Long)
    data class Sample(val sequence: Long, val elapsedMs: Long, val wallMs: Long,
        val phase: String = "", val replacements: Int = -1, val pinned: Boolean = false,
        val failureAbsent: Boolean = false, val armed: Boolean = false)
    data class Response(val binding: Binding, val nonce: String, val disposing: Boolean?, val samples: List<Sample>)
    private val uuid = Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
    private val digest = Regex("[a-f0-9]{64}")

    fun checkBinding(binding: Binding) = with(binding) {
        require(role in setOf("native", "java")) { "Unknown residency role" }
        require(uuid.matches(bootId) && uuid.matches(ticket) && uuid.matches(instance)) { "Invalid residency UUID" }
        require(targetPid > 0 && targetStartTime in 1..9_007_199_254_740_991L) { "Invalid residency target" }
        require(revision.matches(Regex("[1-9][0-9]{0,9}-[a-f0-9]{12}-[a-f0-9]{12}")) && digest.matches(scriptSha256)) { "Invalid residency payload identity" }
        require(runtimeDirectory == "/data/local/tmp/impulse-carplay/runtime/$bootId-$targetPid-$targetStartTime") { "Invalid residency runtime directory" }
        require(payloadDirectory == "/data/local/tmp/impulse-carplay/versions/$revision") { "Invalid residency payload directory" }
    }

    fun bindingJson(b: Binding): String {
        checkBinding(b)
        return """{"schema":1,"role":"${b.role}","bootId":"${b.bootId}","targetPid":${b.targetPid},"targetStartTime":${b.targetStartTime},"revision":"${b.revision}","ticket":"${b.ticket}","instance":"${b.instance}","scriptSha256":"${b.scriptSha256}","runtimeDirectory":"${b.runtimeDirectory}","payloadDirectory":"${b.payloadDirectory}"}"""
    }
    fun parameters(b: Binding) = "{\"impulseResidency\":" + bindingJson(b) + "}"
    fun arguments(b: Binding, injector: String): List<String> {
        checkBinding(b)
        require(injector == "${b.payloadDirectory}/cpbridge-inject") { "Unexpected residency injector" }
        return listOf(injector, "-p", b.targetPid.toString(), "-s", b.scriptPath, "-R", "qjs", "-e", "-P", parameters(b))
    }
    fun commandSha256(b: Binding) = sha256((arguments(b, "${b.payloadDirectory}/cpbridge-inject").joinToString("\u0000") + "\u0000").toByteArray(Charsets.UTF_8))
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun checkExit(expected: Binding, launch: Receipt, exit: Receipt) {
        checkBinding(expected)
        require(launch.binding == expected && exit.binding == expected) { "Residency launch identity mismatch" }
        require(launch.childPid > 0 && launch.childStartTime >= expected.targetStartTime &&
            launch.childPid == exit.childPid && launch.childStartTime == exit.childStartTime) { "Residency child identity mismatch" }
        require(launch.commandSha256 == commandSha256(expected) && exit.commandSha256 == launch.commandSha256) { "Residency declared CLI arguments mismatch" }
        require(launch.commandEvidence == "declared-before-exec-v1" && exit.commandEvidence == launch.commandEvidence &&
            launch.supervisorSha256 == supervisorSha256(expected) && exit.supervisorSha256 == launch.supervisorSha256) {
            "Residency pre-exec supervisor evidence mismatch"
        }
        require(exit.exitCode == 0 && (exit.exitElapsedMs ?: 0) > 0) { "Residency CLI did not exit successfully" }
    }

    fun checkResponse(expected: Binding, launch: Receipt, exit: Receipt, challenge: Challenge,
        response: Response, nowElapsedMs: Long, nowWallMs: Long): Sample {
        checkExit(expected, launch, exit)
        require(uuid.matches(challenge.nonce) && challenge.instance == expected.instance) { "Invalid residency challenge" }
        require(challenge.issuedElapsedMs >= requireNotNull(exit.exitElapsedMs) &&
            challenge.issuedElapsedMs <= nowElapsedMs) { "Residency challenge predates CLI exit" }
        require(response.binding == expected && response.nonce == challenge.nonce && response.disposing == false) { "Residency response identity or disposal mismatch" }
        require(response.samples.size == 2) { "Two residency samples required" }
        val first = response.samples[0]
        val last = response.samples[1]
        require(first.sequence > 0 && last.sequence > first.sequence &&
            last.elapsedMs - first.elapsedMs >= 100) { "Residency samples did not advance" }
        for (sample in response.samples) {
            require(sample.elapsedMs >= challenge.issuedElapsedMs &&
                nowElapsedMs - sample.elapsedMs in 0..3000 &&
                nowWallMs - sample.wallMs in 0..3000) { "Residency response not fresh after challenge" }
            require(if (expected.role == "native") nativeArmed(sample) || nativeInstalled(sample) else sample.armed) {
                "Resident peer state is not prepared"
            }
        }
        return last
    }

    fun nativeArmed(sample: Sample) = sample.phase == "armed" && sample.replacements == 0 && sample.pinned && sample.failureAbsent
    fun nativeInstalled(sample: Sample) = sample.phase == "installed-pinned" && sample.replacements == 9 && sample.pinned && sample.failureAbsent

    fun supervisorSha256(binding: Binding) = sha256(supervisor(binding).toByteArray(Charsets.UTF_8))

    /** A pre-exec barrier makes birth observable even when the CLI returns immediately.
     * commandSha256 declares the exact exec arguments; it is NOT a live argv observation.
     * The immutable supervisor source and payload hashes bind that declaration.
     */
    fun supervisor(binding: Binding): String {
        val args = arguments(binding, "${binding.payloadDirectory}/cpbridge-inject").joinToString(" ", transform = ::quote)
        val run = binding.runtimeDirectory
        val role = binding.role
        val base = bindingJson(binding).dropLast(1)
        val hash = commandSha256(binding)
        val start = "$run/$role-launch.json"
        val end = "$run/$role-exit.json"
        val release = "$run/$role-exec-${binding.instance}.release"
        // installPayload attests the immutable injector hash and size before the cold gate.
        return """
            #!/system/bin/sh
            umask 077
            test ! -e ${quote(start)} && test ! -e ${quote(end)} && test ! -e ${quote(release)} || exit 91
            cpb_supervisor_sha=§(sha256sum "§0" | cut -d' ' -f1)
            test "§{#cpb_supervisor_sha}" = 64 || exit 93
            test "§(sha256sum ${quote(binding.scriptPath)} | cut -d' ' -f1)" = ${quote(binding.scriptSha256)} || exit 95
            (
              cpb_wait=0
              while [ ! -f ${quote(release)} ]; do
                cpb_wait=§((cpb_wait+1))
                test "§cpb_wait" -le 200 || exit 92
                sleep 0.01
              done
              test "§(stat -c %u ${quote(release)})" = 0 && test "§(stat -c %a ${quote(release)})" = 400 || exit 96
              test "§(cat ${quote(release)})" = ${quote(binding.instance)} || exit 97
              exec $args
            ) > ${quote("$run/$role-injector.log")} 2>&1 < /dev/null &
            cpb_child=§!
            cpb_start=0
            cpb_stat=§(cat /proc/§cpb_child/stat 2>/dev/null)
            cpb_tail=§{cpb_stat##*) }; set -- §cpb_tail
            cpb_alive=yes
            case "§1" in ''|Z|X|x) cpb_alive=;; esac
            if [ "§#" -ge 20 ] && [ "§cpb_alive" = yes ]; then
              shift 19; cpb_start=§1
              case "§cpb_start" in ''|*[!0-9]*) cpb_start=0;; esac
            fi
            if [ "§cpb_start" -gt 0 ]; then
              if printf '%s,"childPid":%s,"childStartTime":%s,"commandSha256":"%s","commandEvidence":"declared-before-exec-v1","supervisorSha256":"%s"}\n' ${quote(base)} "§cpb_child" "§cpb_start" ${quote(hash)} "§cpb_supervisor_sha" > ${quote("$start.next")} &&
                  chmod 400 ${quote("$start.next")} && chown 0:0 ${quote("$start.next")} && mv ${quote("$start.next")} ${quote(start)}; then
                printf '%s\n' ${quote(binding.instance)} > ${quote("$release.next")} &&
                  chmod 400 ${quote("$release.next")} && chown 0:0 ${quote("$release.next")} && mv ${quote("$release.next")} ${quote(release)}
              fi
            fi
            wait "§cpb_child"
            cpb_exit=§?
            cpb_elapsed=§(awk '{print int(§1*1000)}' /proc/uptime)
            printf '%s,"childPid":%s,"childStartTime":%s,"commandSha256":"%s","commandEvidence":"declared-before-exec-v1","supervisorSha256":"%s","exitCode":%s,"exitElapsedMs":%s}\n' ${quote(base)} "§cpb_child" "§cpb_start" ${quote(hash)} "§cpb_supervisor_sha" "§cpb_exit" "§cpb_elapsed" > ${quote("$end.next")} &&
            chmod 400 ${quote("$end.next")} && chown 0:0 ${quote("$end.next")} && mv ${quote("$end.next")} ${quote(end)}
        """.trimIndent().replace('§', '$') + "\n"
    }
    fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"
}
