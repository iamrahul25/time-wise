package com.nightwise.timewise.data.profile

import com.nightwise.timewise.data.usage.DayUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileLogicTest {

    private val hour = 60 * 60 * 1000L
    private val today = 100L

    private fun day(epochDay: Long, hours: Double) =
        DayUsage(epochDay, (hours * hour).toLong(), emptyList(), null, List(24) { 0 })

    @Test
    fun levelsFollowDailyAverage() {
        assertEquals(UsageProfile.MONK, ProfileLogic.profileFor(2 * hour - 1))
        assertEquals(UsageProfile.MINDFUL, ProfileLogic.profileFor(2 * hour))
        assertEquals(UsageProfile.HOOKED, ProfileLogic.profileFor(5 * hour))
        assertEquals(UsageProfile.ADDICT, ProfileLogic.profileFor(7 * hour))
        assertEquals(UsageProfile.ZOMBIE, ProfileLogic.profileFor(8 * hour))
        assertEquals(UsageProfile.ZOMBIE, ProfileLogic.profileFor(20 * hour))
    }

    @Test
    fun averageUsesFinishedDaysWithData() {
        val history = listOf(day(today - 3, 4.0), day(today - 2, 0.0), day(today - 1, 6.0), day(today, 15.0))
        val summary = ProfileLogic.summarize(history, today)!!
        assertEquals(5 * hour, summary.dailyAverageMillis)
        assertEquals(2, summary.trackedDays)
        assertEquals(UsageProfile.HOOKED, summary.profile)
        assertNull(summary.previousDailyAverageMillis)
    }

    @Test
    fun comparesWithTheWeekBefore() {
        val history = (today - 14 until today).map { day(it, if (it < today - 7) 6.0 else 3.0) }
        val summary = ProfileLogic.summarize(history, today)!!
        assertEquals(3 * hour, summary.dailyAverageMillis)
        assertEquals(6 * hour, summary.previousDailyAverageMillis)
        assertEquals(UsageProfile.MINDFUL, summary.profile)
    }

    @Test
    fun cutToNextLevelGetsJustUnderItsLimit() {
        val summary = ProfileLogic.summarize((today - 7 until today).map { day(it, 5.0) }, today)!!
        assertEquals(hour + 60_000L, summary.cutToNextLevelMillis)
        assertEquals(76L, summary.daysPerYear)
        val monk = ProfileLogic.summarize(listOf(day(today - 1, 1.0)), today)!!
        assertNull(monk.cutToNextLevelMillis)
    }

    @Test
    fun daysBeforeTrackingStartedDontCount() {
        // Days 86..92 are the older week: only 90..92 are after install, 86..89 are cut-short leftovers.
        val history = (86..99L).map { day(it, if (it < 90) 0.5 else if (it <= 92) 3.5 else 2.0) }
        val summary = ProfileLogic.summarize(history, today, trackingStartDay = 90)!!
        assertEquals(listOf(2, 1), summary.weeklyStatuses.map { it.weeksAgo })
        assertEquals((3.5 * hour).toLong(), summary.weeklyStatuses[0].dailyAverageMillis)
        // The latest week uses all its days, and so does the headline 7-day profile.
        assertEquals(2 * hour, summary.weeklyStatuses[1].dailyAverageMillis)
        assertEquals(2 * hour, summary.dailyAverageMillis)
    }

    @Test
    fun oneWeekOfDataShowsOneBar() {
        val history = (today - 7 until today).map { day(it, 4.0) }
        val summary = ProfileLogic.summarize(history, today, trackingStartDay = today)!!
        assertEquals(listOf(1), summary.weeklyStatuses.map { it.weeksAgo })
    }

    @Test
    fun profileShowsOnInstallDayFromEarlierUsage() {
        val history = (today - 7 until today).map { day(it, 4.0) }
        val summary = ProfileLogic.summarize(history, today, trackingStartDay = today)!!
        assertEquals(4 * hour, summary.dailyAverageMillis)
    }

    @Test
    fun noSummaryWithoutFinishedDays() {
        assertNull(ProfileLogic.summarize(listOf(day(today, 3.0)), today))
    }
}
