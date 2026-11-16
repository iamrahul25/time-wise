package com.nightwise.timewise.data.report

import com.nightwise.timewise.data.usage.DayUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageTimelineTest {

    private val hour = 60 * 60 * 1000L
    private fun day(epoch: Long, millis: Long, unlocks: Int?) =
        DayUsage(epoch, millis, emptyList(), unlocks, List(24) { 0 })

    @Test
    fun dailyRowsAreNewestFirstAndCompareWithTheDayBefore() {
        val history = listOf(day(10, 2 * hour, 30), day(11, 3 * hour, 45), day(12, 0, null))
        val rows = UsageTimeline.rows(TimelinePeriod.DAILY, history, today = 12)

        assertEquals(listOf(11L, 10L), rows.map { it.firstDay })
        assertEquals(3 * hour, rows[0].totals.totalMillis)
        assertEquals(45, rows[0].totals.unlocks)
        assertEquals(2 * hour, rows[0].previous?.totalMillis)
        assertNull(rows[1].previous)
    }

    @Test
    fun weeklyRowsUseFinishedWeeksOnly() {
        // Today is 30. The last finished week is 23..29, and the one before it is 16..22.
        val history = (16L..29L).map { day(it, hour, 10) }
        val rows = UsageTimeline.rows(TimelinePeriod.WEEKLY, history, today = 30)

        assertEquals(23L, rows[0].firstDay)
        assertEquals(29L, rows[0].lastDay)
        assertEquals(7 * hour, rows[0].totals.totalMillis)
        assertEquals(70, rows[0].totals.unlocks)
        assertEquals(7 * hour, rows[0].previous?.totalMillis)
    }
}
