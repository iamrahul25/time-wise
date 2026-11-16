package com.nightwise.timewise.data.bedtime

import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.usage.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class BedtimeInsightsTest {

    private val today = 20_000L
    private val bedtime = 23 * 60
    private val min = 60_000L

    private fun at(day: Long, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        timeInMillis = FocusRepository.startOfDayMillis(day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }.timeInMillis

    private fun session(pkg: String, day: Long, fromHour: Int, fromMinute: Int, toHour: Int, toMinute: Int) =
        UsageSession(pkg, at(day, fromHour, fromMinute), at(day, toHour, toMinute))

    /** An early session so the first nights have data behind them. */
    private val firstSession = session("other", today - 6, 8, 0, 8, 10)
    private val now = at(today, 12, 0)

    @Test
    fun appsAreRankedPerWindowAndAveragedOverCountedNights() {
        val sessions = buildList {
            add(firstSession)
            for (day in (today - 6)..(today - 1)) add(session("b", day, 23, 30, 23, 50))
            for (day in (today - 3)..(today - 1)) add(session("a", day, 22, 0, 22, 30))
        }
        val windows = BedtimeInsights.windows(sessions, bedtime, today, now)

        assertEquals(6, windows.nights)
        val a = windows.before.single()
        assertEquals("a", a.packageName)
        assertEquals(90 * min, a.totalMillis)
        assertEquals(15 * min, a.averageMillis)
        assertEquals(3, a.nightsUsed)
        val b = windows.after.single()
        assertEquals("b", b.packageName)
        assertEquals(20 * min, b.averageMillis)
        assertEquals(6, b.nightsUsed)
        assertEquals(listOf("b", "a"), windows.around.map { it.packageName })
    }

    @Test
    fun lastNightIsTheMostRecentFinishedNightOnItsOwn() {
        val sessions = buildList {
            add(firstSession)
            for (day in (today - 6)..(today - 2)) add(session("a", day, 22, 0, 22, 10))
            add(session("a", today - 1, 22, 0, 22, 40))
        }
        val windows = BedtimeInsights.windows(sessions, bedtime, today, now)
        val last = windows.lastNight!!
        assertEquals(1, last.nights)
        assertEquals(40 * min, last.before.single().averageMillis)
        // Six nights: five of 10 minutes and one of 40, so 15 minutes on average.
        assertEquals(15 * min, windows.before.single().averageMillis)
        assertNull(last.lastNight)
    }

    @Test
    fun sessionAcrossBedtimeIsSplitBetweenTheWindows() {
        val sessions = listOf(firstSession) + (today - 6..today - 1).map { session("a", it, 22, 50, 23, 20) }
        val windows = BedtimeInsights.windows(sessions, bedtime, today, now)
        assertEquals(10 * min, windows.before.single().averageMillis)
        assertEquals(20 * min, windows.after.single().averageMillis)
    }

    @Test
    fun tonightIsLeftOutUntilItsWindowIsOver() {
        val sessions = listOf(firstSession, session("a", today, 22, 0, 22, 30))
        val windows = BedtimeInsights.windows(sessions, bedtime, today, at(today, 21, 0))
        // Six finished nights; tonight's 22:00 session is still ahead of "now" and isn't counted.
        assertEquals(6, windows.nights)
        assertEquals(emptyList<NightAppUsage>(), windows.before)
    }

    @Test
    fun excludedAppsAndNoDataGiveNothing() {
        val sessions = listOf(firstSession) + (today - 6..today - 1).map { session("timewise", it, 22, 0, 22, 30) }
        val windows = BedtimeInsights.windows(sessions, bedtime, today, now, exclude = setOf("timewise"))
        assertEquals(emptyList<NightAppUsage>(), windows.before)
        assertEquals(BedtimeWindows.EMPTY, BedtimeInsights.windows(emptyList(), bedtime, today, now))
    }

    @Test
    fun phoneDownAveragesAcrossMidnight() {
        val times = listOf(at(today, 23, 30), at(today + 1, 0, 30))
        val summary = BedtimeInsights.phoneDown(times)!!
        assertEquals(2, summary.nights)
        assertEquals(0, summary.averageMinutes)
        assertEquals(23 * 60 + 30, summary.earliestMinutes)
        assertEquals(30, summary.latestMinutes)
        assertNull(BedtimeInsights.phoneDown(emptyList()))
    }

    @Test
    fun offsetFromBedtimeCrossesMidnight() {
        assertEquals(120, BedtimeInsights.offsetFromBedtime(60, bedtime))
        assertEquals(-30, BedtimeInsights.offsetFromBedtime(22 * 60 + 30, bedtime))
        assertEquals(0, BedtimeInsights.offsetFromBedtime(bedtime, bedtime))
    }
}
