package com.nightwise.timewise.data.usage

enum class AppPeriod(val label: String, /** Null means all saved history. */ val days: Int?) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    ALL("All time", null)
}

/** What the ranking is ordered by. */
enum class AppMetric(val label: String) {
    TIME("Time used"),
    VISITS("App visits")
}

/** One app's time and visits over a period, with the same-length period before it for a trend. */
data class AppTotal(
    val packageName: String,
    val totalMillis: Long,
    /** Days in the period the app was used at all. */
    val activeDays: Int,
    /** Null for [AppPeriod.ALL], which has nothing before it. */
    val previousMillis: Long?,
    /** Times the app was opened. Days saved before visits were tracked count as 0. */
    val visits: Int = 0,
    val previousVisits: Int? = null
)

data class AppRanking(
    val period: AppPeriod,
    val metric: AppMetric = AppMetric.TIME,
    /** Highest by [metric] first. */
    val apps: List<AppTotal>,
    val screenTimeMillis: Long,
    /** Days in the period with any recorded screen time. */
    val trackedDays: Int,
    val totalVisits: Int = 0
)

data class AppDailyPoint(val epochDay: Long, val millis: Long)

data class AppDetail(
    val packageName: String,
    val todayMillis: Long,
    val weekMillis: Long,
    val previousWeekMillis: Long,
    val monthMillis: Long,
    val allTimeMillis: Long,
    /** Average over every tracked day since the first one on record, counting days the app wasn't opened. */
    val dailyAverageMillis: Long,
    val activeDays: Int,
    val trackedDays: Int,
    /** First day with recorded screen time, where "so far" starts. */
    val sinceDay: Long?,
    val busiestDay: AppDailyPoint?,
    /** This app's share of all screen time so far, 0 to 1. */
    val shareOfScreenTime: Float,
    /** The last 30 days ending today, oldest first, including days the app wasn't used. */
    val daily: List<AppDailyPoint>
)

object AppInsights {

    private const val WEEK = 7
    private const val MONTH = 30

    /** Periods end today, so the current day counts as it fills up. */
    fun ranking(
        history: List<DayUsage>,
        period: AppPeriod,
        today: Long,
        metric: AppMetric = AppMetric.TIME
    ): AppRanking {
        val days = history.filter { it.epochDay <= today }
        val inPeriod = period.days?.let { length -> days.filter { it.epochDay > today - length } } ?: days
        val previous = period.days?.let { length ->
            days.filter { it.epochDay in (today - 2 * length + 1)..(today - length) }
        }
        val previousByApp = previous?.let { prev -> prev.flatMap { it.apps }.groupBy { it.packageName } }
        val all = inPeriod.flatMap { day -> day.apps.map { day.epochDay to it } }
            .groupBy({ it.second.packageName }, { it })
            .map { (pkg, uses) ->
                AppTotal(
                    packageName = pkg,
                    totalMillis = uses.sumOf { it.second.millis },
                    activeDays = uses.filter { it.second.millis > 0 }.map { it.first }.distinct().size,
                    previousMillis = previousByApp?.let { p -> p[pkg].orEmpty().sumOf { it.millis } },
                    visits = uses.sumOf { it.second.visits },
                    previousVisits = previousByApp?.let { p -> p[pkg].orEmpty().sumOf { it.visits } }
                )
            }
        val apps = when (metric) {
            AppMetric.TIME -> all.filter { it.totalMillis > 0 }.sortedByDescending { it.totalMillis }
            AppMetric.VISITS -> all.filter { it.visits > 0 }
                .sortedWith(compareByDescending<AppTotal> { it.visits }.thenByDescending { it.totalMillis })
        }
        return AppRanking(
            period = period,
            metric = metric,
            apps = apps,
            screenTimeMillis = inPeriod.sumOf { it.totalMillis },
            trackedDays = inPeriod.count { it.totalMillis > 0 },
            totalVisits = all.sumOf { it.visits }
        )
    }

    fun detail(history: List<DayUsage>, packageName: String, today: Long): AppDetail {
        val days = history.filter { it.epochDay <= today }
        val byDay = days.associateBy { it.epochDay }
        fun millisOn(day: Long): Long = byDay[day]?.apps?.firstOrNull { it.packageName == packageName }?.millis ?: 0L
        fun sumOver(from: Long, to: Long): Long = (from..to).sumOf(::millisOn)

        val tracked = days.filter { it.totalMillis > 0 }
        val sinceDay = tracked.minOfOrNull { it.epochDay }
        val allTime = tracked.sumOf { millisOn(it.epochDay) }
        val points = tracked.map { AppDailyPoint(it.epochDay, millisOn(it.epochDay)) }
        val screenTime = tracked.sumOf { it.totalMillis }
        return AppDetail(
            packageName = packageName,
            todayMillis = millisOn(today),
            weekMillis = sumOver(today - WEEK + 1, today),
            previousWeekMillis = sumOver(today - 2 * WEEK + 1, today - WEEK),
            monthMillis = sumOver(today - MONTH + 1, today),
            allTimeMillis = allTime,
            dailyAverageMillis = if (tracked.isEmpty()) 0L else allTime / tracked.size,
            activeDays = points.count { it.millis > 0 },
            trackedDays = tracked.size,
            sinceDay = sinceDay,
            busiestDay = points.filter { it.millis > 0 }.maxByOrNull { it.millis },
            shareOfScreenTime = if (screenTime <= 0) 0f else allTime.toFloat() / screenTime,
            daily = ((today - MONTH + 1)..today).map { AppDailyPoint(it, millisOn(it)) }
        )
    }

    /**
     * Each app's average a day over the last 7 finished days, counting days it wasn't opened.
     * Only days with any screen time count, so a new install isn't averaged over empty days.
     */
    fun weeklyDailyAverages(history: List<DayUsage>, today: Long): Map<String, Long> {
        val week = history.filter { it.epochDay in (today - WEEK) until today && it.totalMillis > 0 }
        if (week.isEmpty()) return emptyMap()
        return totalsByApp(week).mapValues { (_, total) -> total / week.size }
    }

    private fun totalsByApp(days: List<DayUsage>): Map<String, Long> =
        days.flatMap { it.apps }.groupBy { it.packageName }.mapValues { (_, uses) -> uses.sumOf { it.millis } }
}
