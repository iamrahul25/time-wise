package com.nightwise.timewise.data.bedtime

import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.usage.UsageSession
import java.util.Calendar
import kotlin.math.roundToInt

/** One app's use in a window around bedtime, averaged over the nights counted. */
data class NightAppUsage(
    val packageName: String,
    /** Average per night, counting nights the app wasn't opened at all. */
    val averageMillis: Long,
    val totalMillis: Long,
    /** Nights, out of [BedtimeWindows.nights], the app was used at all. */
    val nightsUsed: Int
)

data class BedtimeWindows(
    /** Complete nights counted. */
    val nights: Int,
    /** The 2 hours before bedtime, most used first. */
    val before: List<NightAppUsage>,
    /** The 2 hours after bedtime, most used first. */
    val after: List<NightAppUsage>,
    /** Both windows together, the 4 hours around bedtime. */
    val around: List<NightAppUsage>,
    /** Just the most recent finished night, for comparing it with the average. Null for that night itself. */
    val lastNight: BedtimeWindows? = null
) {
    companion object {
        val EMPTY = BedtimeWindows(0, emptyList(), emptyList(), emptyList())
    }
}

/** When the phone usually goes down for the night, from the sleep log. All values are minutes after midnight. */
data class PhoneDownSummary(
    val nights: Int,
    val averageMinutes: Int,
    val earliestMinutes: Int,
    val latestMinutes: Int
)

object BedtimeInsights {

    const val SPAN_MINUTES = 120
    /** Android keeps about 10 days of detailed events, so a week of nights is what can be rebuilt. */
    const val NIGHTS = 7

    private const val MINUTE_MS = 60_000L
    private const val DAY_MINUTES = 24 * 60
    /** Phone-down times before this hour belong to the night before, so 00:40 sits after 23:30. */
    private const val NIGHT_CUTOFF_MINUTES = 15 * 60
    /** An app has to add up to a minute over the nights to be listed. */
    private const val MIN_TOTAL_MS = MINUTE_MS

    /**
     * Which apps took the time in the 2 hours before and after [bedtimeMinutes], over the last
     * [NIGHTS] nights that are fully over. Nights from before the first recorded session aren't counted,
     * so a new install isn't averaged over days with no data.
     */
    fun windows(
        sessions: List<UsageSession>,
        bedtimeMinutes: Int,
        today: Long,
        now: Long,
        exclude: Set<String> = emptySet()
    ): BedtimeWindows {
        val firstData = sessions.minOfOrNull { it.start } ?: return BedtimeWindows.EMPTY
        val span = SPAN_MINUTES * MINUTE_MS
        val bedtimes = ((today - NIGHTS + 1)..today)
            .map { bedtimeAt(it, bedtimeMinutes) }
            .filter { it + span <= now && it - span >= firstData }
        if (bedtimes.isEmpty()) return BedtimeWindows.EMPTY

        val kept = sessions.filter { it.packageName !in exclude }
        return build(kept, bedtimes, span).copy(lastNight = build(kept, listOf(bedtimes.last()), span))
    }

    private fun build(sessions: List<UsageSession>, bedtimes: List<Long>, span: Long) = BedtimeWindows(
        nights = bedtimes.size,
        before = rank(bedtimes.map { usageIn(sessions, it - span, it) }),
        after = rank(bedtimes.map { usageIn(sessions, it, it + span) }),
        around = rank(bedtimes.map { usageIn(sessions, it - span, it + span) })
    )

    /** Average, earliest and latest time the phone was put down, or null with no nights yet. */
    fun phoneDown(downTimes: List<Long>): PhoneDownSummary? {
        if (downTimes.isEmpty()) return null
        val shifted = downTimes.map { nightMinutes(minuteOfDay(it)) }
        return PhoneDownSummary(
            nights = downTimes.size,
            averageMinutes = shifted.average().roundToInt() % DAY_MINUTES,
            earliestMinutes = shifted.min() % DAY_MINUTES,
            latestMinutes = shifted.max() % DAY_MINUTES
        )
    }

    /** How far [minutes] is from [bedtimeMinutes], positive when later, across midnight. */
    fun offsetFromBedtime(minutes: Int, bedtimeMinutes: Int): Int =
        Math.floorMod(minutes - bedtimeMinutes + DAY_MINUTES / 2, DAY_MINUTES) - DAY_MINUTES / 2

    fun minuteOfDay(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    /** Clock minutes laid out along one night, so 00:40 (1480) comes after 23:30 (1410). */
    fun nightMinutes(minutes: Int): Int = if (minutes < NIGHT_CUTOFF_MINUTES) minutes + DAY_MINUTES else minutes

    /**
     * The night a moment belongs to, named after the day its evening starts: anything before 3 PM
     * is still the night before, so 00:40 on Tuesday is Monday's night.
     */
    fun nightDay(timestamp: Long): Long =
        FocusRepository.currentEpochDay(timestamp - NIGHT_CUTOFF_MINUTES * MINUTE_MS)

    /** The moment bedtime falls in the night named [nightDay]; a bedtime after midnight lands on the next calendar day. */
    private fun bedtimeAt(nightDay: Long, bedtimeMinutes: Int): Long {
        val minutes = nightMinutes(bedtimeMinutes)
        return Calendar.getInstance().apply {
            timeInMillis = FocusRepository.startOfDayMillis(nightDay)
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Phone time in the 2 hours after bedtime for each of the last [NIGHTS] finished nights that have
     * data, keyed by [nightDay]. Used to keep a lasting record, since Android drops old events.
     */
    fun afterBedtimeByNight(
        sessions: List<UsageSession>,
        bedtimeMinutes: Int,
        today: Long,
        now: Long,
        exclude: Set<String> = emptySet()
    ): Map<Long, Long> {
        val firstData = sessions.minOfOrNull { it.start } ?: return emptyMap()
        val span = SPAN_MINUTES * MINUTE_MS
        val kept = sessions.filter { it.packageName !in exclude }
        return ((today - NIGHTS)..today).mapNotNull { night ->
            val bedtime = bedtimeAt(night, bedtimeMinutes)
            if (bedtime + span > now || bedtime < firstData) null
            else night to usageIn(kept, bedtime, bedtime + span).values.sum()
        }.toMap()
    }

    private fun usageIn(sessions: List<UsageSession>, start: Long, end: Long): Map<String, Long> {
        val perApp = mutableMapOf<String, Long>()
        for (session in sessions) {
            val overlap = minOf(session.end, end) - maxOf(session.start, start)
            if (overlap > 0) perApp.merge(session.packageName, overlap, Long::plus)
        }
        return perApp
    }

    private fun rank(perNight: List<Map<String, Long>>): List<NightAppUsage> {
        val nights = perNight.size
        val totals = mutableMapOf<String, Long>()
        val nightsUsed = mutableMapOf<String, Int>()
        for (night in perNight) {
            for ((pkg, millis) in night) {
                totals.merge(pkg, millis, Long::plus)
                nightsUsed.merge(pkg, 1, Int::plus)
            }
        }
        return totals
            .filter { it.value >= MIN_TOTAL_MS }
            .map { (pkg, total) -> NightAppUsage(pkg, total / nights, total, nightsUsed.getValue(pkg)) }
            .sortedWith(compareByDescending<NightAppUsage> { it.totalMillis }.thenBy { it.packageName })
    }
}
