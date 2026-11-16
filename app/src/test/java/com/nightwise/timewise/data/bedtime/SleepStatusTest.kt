package com.nightwise.timewise.data.bedtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepStatusTest {

    private fun clock(hour: Int, minute: Int = 0) = hour * 60 + minute

    @Test
    fun statusFollowsTheAveragePhoneDownTime() {
        assertEquals(SleepStatus.EARLY_BIRD, SleepStatusLogic.statusForClock(clock(21, 45)))
        assertEquals(SleepStatus.SANDMAN, SleepStatusLogic.statusForClock(clock(22, 30)))
        assertEquals(SleepStatus.SANDMAN, SleepStatusLogic.statusForClock(clock(23, 29)))
        assertEquals(SleepStatus.NIGHT_OWL, SleepStatusLogic.statusForClock(clock(23, 30)))
        assertEquals(SleepStatus.NIGHT_OWL, SleepStatusLogic.statusForClock(clock(0, 29)))
        assertEquals(SleepStatus.BAT, SleepStatusLogic.statusForClock(clock(0, 30)))
        assertEquals(SleepStatus.BAT, SleepStatusLogic.statusForClock(clock(1, 59)))
        assertEquals(SleepStatus.VAMPIRE, SleepStatusLogic.statusForClock(clock(2, 0)))
        assertEquals(SleepStatus.VAMPIRE, SleepStatusLogic.statusForClock(clock(3, 59)))
        assertEquals(SleepStatus.GHOST, SleepStatusLogic.statusForClock(clock(4, 0)))
        assertEquals(SleepStatus.GHOST, SleepStatusLogic.statusForClock(clock(6, 30)))
    }

    @Test
    fun noStatusUntilThreeNightsAreRecorded() {
        assertNull(SleepStatusLogic.statusFor(PhoneDownSummary(2, clock(1), clock(0, 30), clock(1, 30))))
        assertEquals(
            SleepStatus.BAT,
            SleepStatusLogic.statusFor(PhoneDownSummary(3, clock(1), clock(0, 30), clock(1, 30)))
        )
    }
}
