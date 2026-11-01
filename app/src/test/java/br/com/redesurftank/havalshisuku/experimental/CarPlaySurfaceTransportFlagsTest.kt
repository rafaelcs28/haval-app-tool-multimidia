package br.com.redesurftank.havalshisuku.experimental

import org.junit.Assert.*
import org.junit.Test

/** The fix: the GRANT/REVOKE guard accepts the Binder driver's TF_ACCEPT_FDS (0x10, confirmed in
 *  field as flags=16) while still rejecting FLAG_ONEWAY and any other/unknown flag bit. */
class CarPlaySurfaceTransportFlagsTest {

    @Test fun constant_is_tf_accept_fds() {
        assertEquals(0x10, CarPlaySurfaceEndpoint.TF_ACCEPT_FDS)
    }

    @Test fun accepts_zero_and_tf_accept_fds_only() {
        assertFalse(CarPlaySurfaceEndpoint.transactionFlagsRejected(0))        // plain sync transact
        assertFalse(CarPlaySurfaceEndpoint.transactionFlagsRejected(0x10))     // TF_ACCEPT_FDS alone (field value 16)
    }

    @Test fun rejects_oneway_and_unknown_bits() {
        assertTrue(CarPlaySurfaceEndpoint.transactionFlagsRejected(0x1))       // FLAG_ONEWAY
        assertTrue(CarPlaySurfaceEndpoint.transactionFlagsRejected(0x11))      // oneway + TF_ACCEPT_FDS
        assertTrue(CarPlaySurfaceEndpoint.transactionFlagsRejected(0x20))      // unknown bit
        assertTrue(CarPlaySurfaceEndpoint.transactionFlagsRejected(0x7fffffff))// anything else
    }
}
