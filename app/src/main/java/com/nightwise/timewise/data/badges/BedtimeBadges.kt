package com.nightwise.timewise.data.badges

import com.nightwise.timewise.data.bedtime.BedtimeEvents
import com.nightwise.timewise.data.bedtime.BedtimeInsights
import com.nightwise.timewise.data.bedtime.SleepStatus
import java.util.Calendar
import kotlin.math.abs

/** One recorded night: when the phone went down and when it was picked up. [nightDay] is a [BedtimeInsights.nightDay]. */
data class BedtimeNight(val nightDay: Long, val phoneDownAt: Long, val pickedUpAt: Long) {
    val durationMillis: Long get() = pickedUpAt - phoneDownAt
}

data class BedtimeBadgeInput(
    val nights: List<BedtimeNight>,
    val bedtimeMinutes: Int,
    val sleepGoalMinutes: Int,
    val events: BedtimeEvents,
    val firstDayOfWeek: Int = Calendar.MONDAY
)

/**
 * Judges the bedtime badges. Like [BadgeEngine] it returns the periods a badge was earned in, so the
 * same ledger can keep them: the night for a one-night badge, the first night of a run for a streak,
 * and the first day of the calendar week for a weekly one.
 */
object BedtimeBadgeEngine {

    private const val MINUTE_MS = 60_000L
    private const val ON_TIME_MINUTES = 15
    private const val LATE_MINUTES = 30
    private const val AFTER_LIGHT_MS = 30 * MINUTE_MS
    private const val AFTER_HEAVY_MS = 60 * MINUTE_MS
    private const val GOAL_NIGHTS_IN_WEEK = 5
    private const val DEBT_NIGHTS_IN_WEEK = 3
    private const val HEAVY_NIGHTS_IN_WEEK = 3
    private const val BYPASS_UNLOCKS = 3
    /** A week's average needs at least this many nights, so one late night can't make a Ghost Week. */
    private const val MIN_WEEK_NIGHTS = 4

    fun earnedPeriods(badge: UsageBadge, input: BedtimeBadgeInput): Set<Long> {
        val rule = badge.rule as? BadgeRule.Bedtime ?: return emptySet()
        val events = input.events
        return when (rule.check) {
            BedtimeCheck.ON_TIME ->
                runs(input.nights.filter { abs(input.offset(it)) <= ON_TIME_MINUTES }.days(), rule.streakDays)
            BedtimeCheck.LATE ->
                input.nights.filter { input.offset(it) in LATE_MINUTES until HALF_DAY_MINUTES }.days()
            BedtimeCheck.SLEEP_GOAL ->
                runs(input.nights.filter { input.metGoal(it) }.days(), rule.streakDays)
            BedtimeCheck.WEEK_SLEEP_GOAL ->
                weeksWith(input.nights.filter { input.metGoal(it) }.days(), GOAL_NIGHTS_IN_WEEK, input.firstDayOfWeek)
            BedtimeCheck.WEEK_SLEEP_DEBT ->
                weeksWith(input.nights.filter { !input.metGoal(it) }.days(), DEBT_NIGHTS_IN_WEEK, input.firstDayOfWeek)
            BedtimeCheck.AFTER_30 ->
                events.afterBedtimeMillis.filterValues { it >= AFTER_LIGHT_MS }.keys
            BedtimeCheck.WEEK_AFTER_60 ->
                weeksWith(events.afterBedtimeMillis.filterValues { it >= AFTER_HEAVY_MS }.keys, HEAVY_NIGHTS_IN_WEEK, input.firstDayOfWeek)
            BedtimeCheck.VAULT -> {
                val unlocked = events.unlockTimes.map(BedtimeInsights::nightDay).toSet()
                runs(events.focusNights - unlocked, rule.streakDays)
            }
            BedtimeCheck.KEPT -> events.keptTimes.map(BedtimeInsights::nightDay).toSet()
            BedtimeCheck.BYPASS -> events.unlockTimes.groupBy(BedtimeInsights::nightDay)
                .filterValues { it.size >= BYPASS_UNLOCKS }.keys
            BedtimeCheck.FOCUS_OFF -> events.focusOffTimes.map(BedtimeInsights::nightDay).toSet()
            BedtimeCheck.VAMPIRE ->
                runs(input.nights.filter { nightMinutes(it) >= SleepStatus.BAT.maxNightMinutes }.days(), rule.streakDays)
            BedtimeCheck.WEEK_BAT -> weeksAveraging(input, SleepStatus.NIGHT_OWL.maxNightMinutes)
            BedtimeCheck.WEEK_GHOST -> weeksAveraging(input, SleepStatus.VAMPIRE.maxNightMinutes)
        }
    }

    private const val HALF_DAY_MINUTES = 12 * 60

    /** Minutes the phone went down after or before the bedtime that was set, positive when later. */
    private fun BedtimeBadgeInput.offset(night: BedtimeNight): Int =
        BedtimeInsights.offsetFromBedtime(BedtimeInsights.minuteOfDay(night.phoneDownAt), bedtimeMinutes)

    private fun BedtimeBadgeInput.metGoal(night: BedtimeNight): Boolean =
        night.durationMillis >= sleepGoalMinutes * MINUTE_MS

    private fun nightMinutes(night: BedtimeNight): Int =
        BedtimeInsights.nightMinutes(BedtimeInsights.minuteOfDay(night.phoneDownAt))

    private fun List<BedtimeNight>.days(): Set<Long> = mapTo(HashSet()) { it.nightDay }

    /** The nights of [days] that begin each back-to-back block of [length] nights. A [length] of 1 keeps every night. */
    private fun runs(days: Set<Long>, length: Int): Set<Long> {
        if (length <= 1) return days
        val starts = mutableSetOf<Long>()
        var runStart = 0L
        var previous: Long? = null
        for (day in days.sorted()) {
            if (previous == null || day != previous + 1) runStart = day
            if ((day - runStart + 1) % length == 0L) starts += day - length + 1
            previous = day
        }
        return starts
    }

    private fun weekStart(night: Long, firstDayOfWeek: Int): Long =
        BadgeEngine.spanRange(Span.WEEK, night, firstDayOfWeek).first

    /** Weeks, by their first day, that hold at least [count] of the [nights]. */
    private fun weeksWith(nights: Collection<Long>, count: Int, firstDayOfWeek: Int): Set<Long> =
        nights.groupBy { weekStart(it, firstDayOfWeek) }.filterValues { it.size >= count }.keys

    /** Weeks with enough nights whose average phone-down time is at or after [thresholdNightMinutes]. */
    private fun weeksAveraging(input: BedtimeBadgeInput, thresholdNightMinutes: Int): Set<Long> =
        input.nights.groupBy { weekStart(it.nightDay, input.firstDayOfWeek) }
            .filterValues { week -> week.size >= MIN_WEEK_NIGHTS && week.map(::nightMinutes).average() >= thresholdNightMinutes }
            .keys
}
