package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SunroofCurtainSchedulePolicyTest {
    @Test
    public void closeWindowBlocksOpenForEntireOverlap() {
        assertEquals(SunroofCurtainSchedulePolicy.Action.CLOSE,
                SunroofCurtainSchedulePolicy.actionFor(true, false, true, false));
        assertEquals(SunroofCurtainSchedulePolicy.Action.NONE,
                SunroofCurtainSchedulePolicy.actionFor(true, false, true, true));
    }

    @Test
    public void openRunsOnlyWhenCloseWindowIsInactiveAndOpenWasNotHandled() {
        assertEquals(SunroofCurtainSchedulePolicy.Action.OPEN,
                SunroofCurtainSchedulePolicy.actionFor(true, false, false, false));
        assertEquals(SunroofCurtainSchedulePolicy.Action.NONE,
                SunroofCurtainSchedulePolicy.actionFor(true, true, false, false));
    }
}
