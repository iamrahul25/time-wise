package com.nightwise.timewise.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BedtimeWindowTest {

    private fun at(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.MARCH, 10, hour, minute, 0)
    }.timeInMillis

    @Test
    fun overnightWindowCoversLateNightAndEarlyMorning() {
        val settings = FocusSettings(bedtimeMinutes = 23 * 60, wakeMinutes = 7 * 60)
        assertTrue(settings.isBedtime(at(23, 0)))
        assertTrue(settings.isBedtime(at(2, 30)))
        assertFalse(settings.isBedtime(at(7, 0)))
        assertFalse(settings.isBedtime(at(12, 0)))
    }

    @Test
    fun bedtimeFocusIsOnlyOnWithEveryStepSetUp() {
        val on = FocusSettings(bedtimeFocusEnabled = true)
        assertTrue(on.isBedtimeFocusOn(allStepsReady = true))
        assertFalse(on.isBedtimeFocusOn(allStepsReady = false))
        assertFalse(FocusSettings(bedtimeFocusEnabled = false).isBedtimeFocusOn(allStepsReady = true))
    }

    @Test
    fun sameDayWindowDoesNotWrap() {
        val settings = FocusSettings(bedtimeMinutes = 13 * 60, wakeMinutes = 15 * 60)
        assertTrue(settings.isBedtime(at(14, 0)))
        assertFalse(settings.isBedtime(at(12, 0)))
        assertFalse(settings.isBedtime(at(15, 0)))
    }
}
