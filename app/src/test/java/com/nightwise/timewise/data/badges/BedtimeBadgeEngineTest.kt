package com.nightwise.timewise.data.badges

import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.bedtime.BedtimeEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BedtimeBadgeEngineTest {

    private val bedtime = 23 * 60
    private val monday = (20_000L..20_006L).first { Math.floorMod(it + 3, 7L) == 0L }
    private val hour = 60 * 60 * 1000L

    private fun at(day: Long, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        timeInMillis = FocusRepository.startOfDayMillis(day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }.timeInMillis

    /** Phone down at [hour]:[minute] on the evening of [night] (after midnight lands on the next calendar day), up [slept] hours later. */
    private fun night(night: Long, hour: Int, minute: Int, slept: Double = 8.0): BedtimeNight {
        val down = at(if (hour < 15) night + 1 else night, hour, minute)
        return BedtimeNight(night, down, down + (slept * this.hour).toLong())
    }

    private fun input(nights: List<BedtimeNight> = emptyList(), events: BedtimeEvents = BedtimeEvents()) =
        BedtimeBadgeInput(nights, bedtime, sleepGoalMinutes = 8 * 60, events = events, firstDayOfWeek = Calendar.MONDAY)

    private fun earned(badge: UsageBadge, input: BedtimeBadgeInput) = BedtimeBadgeEngine.earnedPeriods(badge, input)

    @Test
    fun goodNightCountsEveryNightWithinFifteenMinutes() {
        val nights = listOf(night(100, 22, 50), night(101, 23, 10), night(102, 23, 40))
        assertEquals(setOf(100L, 101L), earned(UsageBadge.GOOD_NIGHT, input(nights)))
    }

    @Test
    fun clockworkNeedsSevenNightsInARow() {
        val week = (100L..106L).map { night(it, 23, 5) }
        assertEquals(setOf(100L), earned(UsageBadge.CLOCKWORK, input(week)))
        assertTrue(earned(UsageBadge.CLOCKWORK, input(week.drop(1))).isEmpty())
        val broken = week.filterNot { it.nightDay == 103L }
        assertTrue(earned(UsageBadge.CLOCKWORK, input(broken)).isEmpty())
        assertEquals(setOf(100L, 107L), earned(UsageBadge.CLOCKWORK, input((100L..113L).map { night(it, 22, 55) })))
    }

    @Test
    fun justOneMoreNeedsThirtyMinutesPastBedtime() {
        val nights = listOf(night(100, 23, 40), night(101, 23, 20), night(102, 2, 0), night(103, 22, 0))
        assertEquals(setOf(100L, 102L), earned(UsageBadge.JUST_ONE_MORE, input(nights)))
    }

    @Test
    fun sleepGoalBadgesUseTheTimeSlept() {
        val rested = (100L..106L).map { night(it, 23, 0, slept = 8.0) }
        assertEquals(setOf(100L), earned(UsageBadge.FULLY_CHARGED, input(rested)))
        val short = rested.map { it.copy(pickedUpAt = it.phoneDownAt + 7 * hour) }
        assertTrue(earned(UsageBadge.FULLY_CHARGED, input(short)).isEmpty())
    }

    @Test
    fun wellRestedAndSleepDebtCountNightsInACalendarWeek() {
        val fiveGood = (0L..4L).map { night(monday + it, 23, 0, slept = 8.0) } +
            (5L..6L).map { night(monday + it, 23, 0, slept = 5.0) }
        assertEquals(setOf(monday), earned(UsageBadge.WELL_RESTED, input(fiveGood)))
        assertTrue(earned(UsageBadge.SLEEP_DEBT, input(fiveGood)).isEmpty())

        val threeShort = (0L..2L).map { night(monday + it, 23, 0, slept = 5.0) } +
            (3L..6L).map { night(monday + it, 23, 0, slept = 8.0) }
        assertEquals(setOf(monday), earned(UsageBadge.SLEEP_DEBT, input(threeShort)))
        assertTrue(earned(UsageBadge.WELL_RESTED, input(threeShort)).isEmpty())
    }

    @Test
    fun phoneTimeAfterBedtimeFeedsSnoozeScrollerAndNightRaider() {
        val minutes = { m: Long -> m * 60_000L }
        val after = mapOf(
            monday to minutes(35),
            monday + 1 to minutes(70),
            monday + 2 to minutes(90),
            monday + 3 to minutes(20),
            monday + 4 to minutes(61)
        )
        val events = BedtimeEvents(afterBedtimeMillis = after)
        assertEquals(setOf(monday, monday + 1, monday + 2, monday + 4), earned(UsageBadge.SNOOZE_SCROLLER, input(events = events)))
        assertEquals(setOf(monday), earned(UsageBadge.NIGHT_RAIDER, input(events = events)))
        val two = BedtimeEvents(afterBedtimeMillis = mapOf(monday to minutes(90), monday + 1 to minutes(90)))
        assertTrue(earned(UsageBadge.NIGHT_RAIDER, input(events = two)).isEmpty())
    }

    @Test
    fun vaultDoorNeedsFourteenFocusNightsWithoutAnUnlock() {
        val nights = (100L..113L).toSet()
        assertEquals(setOf(100L), earned(UsageBadge.VAULT_DOOR, input(events = BedtimeEvents(focusNights = nights))))
        val unlocked = BedtimeEvents(focusNights = nights, unlockTimes = listOf(at(105, 23, 30)))
        assertTrue(earned(UsageBadge.VAULT_DOOR, input(events = unlocked)).isEmpty())
        // Nights the focus wasn't on don't count, even with no unlocks.
        assertTrue(earned(UsageBadge.VAULT_DOOR, input(events = BedtimeEvents(focusNights = (100L..110L).toSet()))).isEmpty())
    }

    @Test
    fun focusEventsBecomeTheirNightsBadges() {
        val events = BedtimeEvents(
            keptTimes = listOf(at(100, 23, 30), at(101, 1, 0)),
            focusOffTimes = listOf(at(102, 23, 15)),
            unlockTimes = listOf(at(103, 23, 5), at(103, 23, 20), at(103, 23, 45), at(104, 23, 5), at(104, 23, 20))
        )
        // 01:00 on day 101 still belongs to the night of day 100.
        assertEquals(setOf(100L), earned(UsageBadge.PROMISE_KEPT, input(events = events)))
        assertEquals(setOf(102L), earned(UsageBadge.BROKEN_PROMISE, input(events = events)))
        assertEquals(setOf(103L), earned(UsageBadge.BYPASS_ARTIST, input(events = events)))
    }

    @Test
    fun lateNightBadgesFollowTheClock() {
        val vampireWeek = (100L..106L).map { night(it, 2, 30) }
        assertEquals(setOf(100L), earned(UsageBadge.VAMPIRE_HABIT, input(vampireWeek)))
        assertTrue(earned(UsageBadge.VAMPIRE_HABIT, input((100L..106L).map { night(it, 1, 30) })).isEmpty())

        val batWeek = (0L..3L).map { night(monday + it, 1, 0) }
        assertEquals(setOf(monday), earned(UsageBadge.BAT_WEEK, input(batWeek)))
        assertTrue(earned(UsageBadge.GHOST_WEEK, input(batWeek)).isEmpty())

        val ghostWeek = (0L..3L).map { night(monday + it, 4, 30) }
        assertEquals(setOf(monday), earned(UsageBadge.GHOST_WEEK, input(ghostWeek)))
        // Three nights is too few to call a week.
        assertTrue(earned(UsageBadge.GHOST_WEEK, input(ghostWeek.take(3))).isEmpty())
    }

    @Test
    fun aRunKeepsItsStartWhenItGrows() {
        val saved = setOf(100L)
        assertEquals(saved, BadgeEngine.merge(UsageBadge.CLOCKWORK, saved, setOf(101L)))
        assertEquals(setOf(100L, 107L), BadgeEngine.merge(UsageBadge.CLOCKWORK, saved, setOf(107L)))
    }

    @Test
    fun everyBedtimeBadgeHasADayHistoryNoOp() {
        val bedtimeBadges = UsageBadge.entries.filter { it.rule is BadgeRule.Bedtime }
        assertEquals(15, bedtimeBadges.size)
        bedtimeBadges.forEach { badge ->
            assertTrue(BadgeEngine.earnedPeriods(badge, emptyList(), 100L).isEmpty())
        }
    }
}
