package br.com.redesurftank.havalshisuku.managers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileDataManagerFirewallTest {
    @Test
    fun subnetOfTheTboxLink() {
        val address = byteArrayOf(172.toByte(), 16, 13, 99.toByte())
        assertEquals("172.16.13.0/24", MobileDataManager.subnetCidr(address, 24))
        assertNull(MobileDataManager.subnetCidr(byteArrayOf(1, 2, 3), 24))
        assertNull(MobileDataManager.subnetCidr(address, 4))
    }

    @Test
    fun onlyPlainInterfaceNamesReachTheShell() {
        assertTrue(MobileDataManager.isSafeIface("vlan13"))
        assertTrue(MobileDataManager.isSafeIface("rmnet_data0"))
        assertFalse(MobileDataManager.isSafeIface("vlan13; reboot"))
        assertFalse(MobileDataManager.isSafeIface("a b"))
    }

    @Test
    fun blockScriptRejectsMobileButKeepsTheTboxSubnet() {
        val script = MobileDataManager.firewallScript(true, "vlan13", "172.16.13.0/24")
        assertTrue(script.contains("-o vlan13 -d 172.16.13.0/24 -j RETURN"))
        assertTrue(script.contains("iptables -A impulse_mobile_block -o vlan13 -j REJECT"))
        assertTrue(script.contains("iptables -I \$t 1 -j impulse_mobile_block"))
        assertTrue(script.contains("NOT_ROOT"))
        assertTrue(script.indexOf("-j RETURN") < script.indexOf("iptables -A impulse_mobile_block -o vlan13 -j REJECT"))
    }

    @Test
    fun unblockScriptRemovesTheChain() {
        val script = MobileDataManager.firewallScript(false, "vlan13", null)
        assertTrue(script.contains("iptables -X impulse_mobile_block"))
        assertFalse(script.contains("REJECT"))
    }
}
