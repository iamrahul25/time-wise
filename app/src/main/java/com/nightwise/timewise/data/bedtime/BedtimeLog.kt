package com.nightwise.timewise.data.bedtime

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** What bedtime focus and the nights themselves have left behind, for the bedtime badges. All days are [BedtimeInsights.nightDay]s. */
data class BedtimeEvents(
    /** Times a blocked app was unlocked at bedtime by going through the steps. */
    val unlockTimes: List<Long> = emptyList(),
    /** Times a blocked app was closed at bedtime instead of unlocked. */
    val keptTimes: List<Long> = emptyList(),
    /** Times bedtime focus was turned off through the steps. */
    val focusOffTimes: List<Long> = emptyList(),
    /** Nights bedtime focus was on and blocking. */
    val focusNights: Set<Long> = emptySet(),
    /** Phone time in the 2 hours after bedtime, by night. */
    val afterBedtimeMillis: Map<Long, Long> = emptyMap()
)

/**
 * A small lasting log in its own SharedPreferences file, so it is backed up with the rest of the app
 * and survives a reinstall. Android only keeps about 10 days of detail, so anything the badges need
 * later is written down here.
 */
class BedtimeLog private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun events(): BedtimeEvents = BedtimeEvents(
        unlockTimes = longs(KEY_UNLOCKS),
        keptTimes = longs(KEY_KEPT),
        focusOffTimes = longs(KEY_FOCUS_OFF),
        focusNights = longs(KEY_FOCUS_NIGHTS).toSet(),
        afterBedtimeMillis = prefs.all
            .filterKeys { it.startsWith(AFTER_PREFIX) }
            .mapNotNull { (key, value) ->
                val night = key.removePrefix(AFTER_PREFIX).toLongOrNull()
                val millis = value as? Long
                if (night == null || millis == null) null else night to millis
            }
            .toMap()
    )

    fun recordUnlock(at: Long = System.currentTimeMillis()) = append(KEY_UNLOCKS, at)

    fun recordKept(at: Long = System.currentTimeMillis()) = append(KEY_KEPT, at)

    fun recordFocusOff(at: Long = System.currentTimeMillis()) = append(KEY_FOCUS_OFF, at)

    @Synchronized
    fun markFocusNight(night: Long) {
        val nights = longs(KEY_FOCUS_NIGHTS)
        if (night in nights) return
        prefs.edit { putStringSet(KEY_FOCUS_NIGHTS, (nights + night).mapTo(HashSet()) { it.toString() }) }
    }

    @Synchronized
    fun saveAfterBedtime(byNight: Map<Long, Long>) {
        if (byNight.isEmpty()) return
        prefs.edit { byNight.forEach { (night, millis) -> putLong(AFTER_PREFIX + night, millis) } }
    }

    @Synchronized
    private fun append(key: String, value: Long) {
        val kept = longs(key).filter { it >= value - RETENTION_MS }
        prefs.edit { putStringSet(key, (kept + value).mapTo(HashSet()) { it.toString() }) }
    }

    private fun longs(key: String): List<Long> =
        prefs.getStringSet(key, null).orEmpty().mapNotNull { it.toLongOrNull() }

    companion object {
        private const val PREFS_NAME = "timewise_bedtime"
        private const val KEY_UNLOCKS = "unlocks"
        private const val KEY_KEPT = "kept"
        private const val KEY_FOCUS_OFF = "focus_off"
        private const val KEY_FOCUS_NIGHTS = "focus_nights"
        private const val AFTER_PREFIX = "after_"
        /** A little over a year, like the saved usage history. */
        private const val RETENTION_MS = 400L * 24 * 60 * 60 * 1000

        @Volatile
        private var instance: BedtimeLog? = null

        fun get(context: Context): BedtimeLog =
            instance ?: synchronized(this) {
                instance ?: BedtimeLog(context.applicationContext).also { instance = it }
            }
    }
}
