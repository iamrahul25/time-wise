package com.nightwise.timewise.data.limits

/** Time allowed in one app each day. */
data class AppLimit(val packageName: String, val minutes: Int)

data class LimitsState(
    /** Limits are only enforced while this is on. */
    val enabled: Boolean = true,
    val limits: List<AppLimit> = emptyList(),
    /** Day the [extras] were earned on; they reset with the day. */
    val extrasDay: Long = -1L,
    /** Extra minutes earned per app on [extrasDay]. */
    val extras: Map<String, Int> = emptyMap()
) {
    fun limitFor(packageName: String): AppLimit? = limits.firstOrNull { it.packageName == packageName }

    fun extraMinutes(packageName: String, epochDay: Long): Int =
        if (epochDay == extrasDay) extras[packageName] ?: 0 else 0

    /** Wait, in minutes, before the next extra time (or a change that dodges the limit) for one app. */
    fun cooldownMinutes(packageName: String, epochDay: Long): Int =
        LimitLogic.cooldownMinutes(extraMinutes(packageName, epochDay))

    /** The longest wait among every app that has a limit; used for turning all limits off. */
    fun maxCooldownMinutes(epochDay: Long): Int =
        limits.maxOfOrNull { cooldownMinutes(it.packageName, epochDay) } ?: LimitLogic.COOLDOWN_STEP_MINUTES
}

object LimitLogic {
    /** What finishing every friction step earns. */
    const val EXTRA_MINUTES = 15
    const val MIN_LIMIT_MINUTES = 5
    const val MAX_LIMIT_MINUTES = 8 * 60
    const val DEFAULT_LIMIT_MINUTES = 30
    val PRESET_MINUTES = listOf(15, 30, 60, 120)

    /** Each extra time earned today for an app makes the next one wait this much longer. */
    const val COOLDOWN_STEP_MINUTES = 5

    /** 5 min for the first extra time of the day, then 10, 15, 20... */
    fun cooldownMinutes(extraMinutesToday: Int): Int =
        (extraMinutesToday / EXTRA_MINUTES + 1) * COOLDOWN_STEP_MINUTES

    /** Raising a limit is the only change that counts as dodging it; lowering or adding is free. */
    fun isRaise(oldMinutes: Int?, newMinutes: Int): Boolean = oldMinutes != null && newMinutes > oldMinutes

    /** Turning limits off only needs the steps when there is something to dodge. */
    fun needsStepsToDisable(state: LimitsState): Boolean = state.enabled && state.limits.isNotEmpty()

    private const val MINUTE_MS = 60_000L
    private const val WARN_BEFORE_MS = 5 * MINUTE_MS
    private const val MIN_CHECK_MS = 5_000L
    private const val MAX_CHECK_MS = 30_000L

    fun allowanceMillis(limitMinutes: Int, extraMinutes: Int): Long = (limitMinutes + extraMinutes) * MINUTE_MS

    fun remainingMillis(usedMillis: Long, limitMinutes: Int, extraMinutes: Int): Long =
        (allowanceMillis(limitMinutes, extraMinutes) - usedMillis).coerceAtLeast(0L)

    fun isExhausted(usedMillis: Long, limitMinutes: Int, extraMinutes: Int): Boolean =
        usedMillis >= allowanceMillis(limitMinutes, extraMinutes)

    /** A heads-up goes out once 5 minutes or less are left. */
    fun shouldWarn(remainingMillis: Long): Boolean = remainingMillis in 1..WARN_BEFORE_MS

    /** Checks again right when the time should run out, but no faster than every 5s or slower than every 30s. */
    fun nextCheckDelay(remainingMillis: Long): Long = remainingMillis.coerceIn(MIN_CHECK_MS, MAX_CHECK_MS)

    /** 5-minute steps up to an hour, then 15-minute steps. */
    fun stepUp(minutes: Int): Int = (minutes + if (minutes < 60) 5 else 15).coerceAtMost(MAX_LIMIT_MINUTES)

    fun stepDown(minutes: Int): Int = (minutes - if (minutes <= 60) 5 else 15).coerceAtLeast(MIN_LIMIT_MINUTES)
}
