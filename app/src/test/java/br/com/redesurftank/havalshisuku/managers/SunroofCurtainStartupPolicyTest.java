package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SunroofCurtainStartupPolicyTest {
    @Test
    public void pollSkipsSettleOnlyWhenReadyWasAlreadyOnAtArm() {
        assertEquals(0L, SunroofCurtainStartupPolicy.settleMsFor(
                SunroofCurtainStartupPolicy.Trigger.DRIVING_READY_POLL, true));
        assertEquals(20_000L, SunroofCurtainStartupPolicy.settleMsFor(
                SunroofCurtainStartupPolicy.Trigger.DRIVING_READY_POLL, false));
    }

    @Test
    public void eventAndFallbackKeepFullSettle() {
        assertEquals(20_000L, SunroofCurtainStartupPolicy.settleMsFor(
                SunroofCurtainStartupPolicy.Trigger.DRIVING_READY_EVENT, true));
        assertEquals(20_000L, SunroofCurtainStartupPolicy.settleMsFor(
                SunroofCurtainStartupPolicy.Trigger.FALLBACK_TIMEOUT, true));
    }
}
