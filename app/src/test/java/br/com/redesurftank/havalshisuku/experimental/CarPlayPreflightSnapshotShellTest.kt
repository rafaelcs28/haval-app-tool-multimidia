package br.com.redesurftank.havalshisuku.experimental

import org.junit.Assert.*
import org.junit.Test

/**
 * Versions the diagnostic instrumentation of the inline environmentSnapshot shell: the per-command
 * exit codes must come from the ORIGINAL read (no repeated commands) and the printf contract must
 * stay balanced. These are string-structure assertions (the shell itself needs a device to run).
 */
class CarPlayPreflightSnapshotShellTest {
    private val shell = CarPlayResidentProof.environmentSnapshot("/data/local/tmp/impulse-carplay/versions/74149-x/cpbridge-inject")

    @Test fun captures_each_hash_exit_from_a_single_run() {
        // sha256sum output goes to a raw var whose own $? is captured BEFORE the cut masks it.
        assertTrue(shell.contains("cpb_env_policy_raw=\$(sha256sum /sys/fs/selinux/policy); cpb_env_policy_rc=\$?"))
        assertTrue(shell.contains("cpb_env_injector_raw=\$(sha256sum "))
        assertTrue(shell.contains("cpb_env_injector_rc=\$?"))
        assertTrue(shell.contains("cpb_env_fingerprint_rc=\$?"))
        // exactly one sha256sum per hash (no diagnostic re-run on the critical path).
        assertEquals(2, Regex("sha256sum ").findAll(shell).count())
        // the stat guard that aborts upstream on failure is preserved.
        assertTrue(shell.contains("test \"\$cpb_env_fingerprint_rc\" = 0 || exit 1"))
    }

    @Test fun emits_the_three_rc_fields() {
        assertTrue(shell.contains("env_policy_sha_rc=%s"))
        assertTrue(shell.contains("env_injector_sha_rc=%s"))
        assertTrue(shell.contains("env_injector_fingerprint_rc=%s"))
    }

    @Test fun printf_format_and_args_stay_balanced() {
        val printf = shell.lineSequence().first { it.startsWith("printf 'env_boot=") }
        val placeholders = Regex("%s").findAll(printf).count()
        val args = Regex("\"\\\$cpb_env_[a-z_]+\"").findAll(printf).count()
        assertEquals(9, placeholders)
        assertEquals(placeholders, args)
    }
}
