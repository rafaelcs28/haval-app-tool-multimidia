package br.com.redesurftank.havalshisuku.managers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RetainedBluetoothDevicePolicyTest {
    private val policy = RetainedBluetoothDevicePolicy
    private fun candidate() = RetainedBluetoothDevicePolicy.Facts(
        type = 1, accepted = 1, state = 0, connections = mapOf(2 to 3), available = 0,
        active = 0, activeStates = emptyMap(), address = "12:34:56:78:9A:BC",
        usbDeviceMode = false, transportNodes = listOf(null, "", null))

    @Test fun pairedOfflinePhonesAreOnlyCandidatesForIndependentRadioProof() {
        assertEquals(candidate().address, policy.addressIfCandidate(candidate()))
        assertEquals(candidate().address, policy.addressIfCandidate(candidate().copy(type = 2)))
    }

    @Test fun unknownDeviceTypesAndNonPhonesAreRejected() {
        for (type in listOf(-1, 0, 3, 4, 5, Int.MAX_VALUE))
            assertNull(policy.addressIfCandidate(candidate().copy(type = type)))
    }

    @Test fun acceptedMustBeExplicitlyOne() {
        for (accepted in listOf(-1, 0, 2))
            assertNull(policy.addressIfCandidate(candidate().copy(accepted = accepted)))
    }

    @Test fun onlyExactOfflineStateIsAdmitted() {
        for (state in listOf(-1, 1, 2, 3, 4, 5))
            assertNull(policy.addressIfCandidate(candidate().copy(state = state)))
    }

    @Test fun bluetoothReadyIsTheOnlyReviewedRetainedConnectionSignature() {
        for (connections in listOf(emptyMap(), mapOf(2 to 0), mapOf(2 to 1), mapOf(2 to 2),
                mapOf(2 to 4), mapOf(2 to 5), mapOf(1 to 3), mapOf(3 to 3), mapOf(4 to 3),
                mapOf(2 to 3, 1 to 4), mapOf(2 to 3, 3 to 4), mapOf(2 to 3, 99 to 0))) {
            assertNull(policy.addressIfCandidate(candidate().copy(connections = connections)))
        }
    }

    @Test fun availableOrActiveCapabilitiesBlockEvenWithOfflineLabel() {
        for (value in listOf(-1, 1, 2, 128, 1024)) {
            assertNull(policy.addressIfCandidate(candidate().copy(available = value)))
            assertNull(policy.addressIfCandidate(candidate().copy(active = value)))
        }
    }

    @Test fun anyActiveStateEntryBlocksIncludingUnknownAndTransitionValues() {
        for (state in 0..5)
            assertNull(policy.addressIfCandidate(candidate().copy(activeStates = mapOf(1024 to state))))
    }

    @Test fun usbDeviceModeAndAnyNativeNodeBlock() {
        assertNull(policy.addressIfCandidate(candidate().copy(usbDeviceMode = true)))
        for (index in 0..2) {
            val nodes = MutableList<String?>(3) { null }.also { it[index] = "/dev/fixture" }
            assertNull(policy.addressIfCandidate(candidate().copy(transportNodes = nodes)))
        }
        assertNull(policy.addressIfCandidate(candidate().copy(transportNodes = emptyList())))
        assertNull(policy.addressIfCandidate(candidate().copy(transportNodes = List(4) { null })))
    }

    @Test fun malformedMissingAndBroadcastAddressesCannotBecomeProxyTargets() {
        for (address in listOf(null, "", "1234", " 12:34:56:78:9A:BC", "12:34:56:78:9A:BC ",
                "GG:34:56:78:9A:BC", "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF")) {
            assertNull(policy.addressIfCandidate(candidate().copy(address = address)))
        }
    }

    @Test fun addressCaseIsNormalizedWithoutChangingIdentity() {
        assertEquals(candidate().address,
            policy.addressIfCandidate(candidate().copy(address = "12:34:56:78:9a:bc")))
    }

    @Test fun everyTransitionAndConnectedProfileBlocks() {
        assertEquals(true, policy.profileAbsence(0))
        for (state in 1..3) assertEquals(false, policy.profileAbsence(state))
        for (state in listOf(-1, 4, 10, 12, Int.MAX_VALUE)) assertNull(policy.profileAbsence(state))
    }
}
