package com.nightwise.timewise.data.bedtime

/**
 * Levels by the average time the phone goes down for the night, earliest first.
 * [maxNightMinutes] is exclusive and measured along one night (see [BedtimeInsights.nightMinutes]),
 * so 24 * 60 + 30 is 12:30 AM.
 */
enum class SleepStatus(val title: String, val tagline: String, val maxNightMinutes: Int) {
    EARLY_BIRD("Early Bird", "Phone down early. You win your mornings the night before.", 22 * 60 + 30),
    SANDMAN("Sandman", "Steady and rested. This is the sweet spot.", 23 * 60 + 30),
    NIGHT_OWL("Night Owl", "Late, but not lost. A little earlier and you're a Sandman.", 24 * 60 + 30),
    BAT("Bat", "Fully nocturnal. The night is when your phone wakes up.", 26 * 60),
    VAMPIRE("Vampire", "Your day starts when the sun goes down. Your body is paying for it.", 28 * 60),
    GHOST("Ghost", "Still haunting the night when the sun comes up. Put the phone down tonight, just once.", Int.MAX_VALUE);

    /** Where this status starts: the end of the one before it, or 0 for [EARLY_BIRD]. */
    val minNightMinutes: Int get() = entries.getOrNull(ordinal - 1)?.maxNightMinutes ?: 0
}

object SleepStatusLogic {

    /** Fewer nights than this is too little to call anyone a Ghost. */
    const val MIN_NIGHTS = 3

    fun statusForClock(averageMinutes: Int): SleepStatus {
        val night = BedtimeInsights.nightMinutes(averageMinutes)
        return SleepStatus.entries.first { night < it.maxNightMinutes }
    }

    /** Null until [MIN_NIGHTS] nights have been recorded. */
    fun statusFor(summary: PhoneDownSummary): SleepStatus? =
        if (summary.nights < MIN_NIGHTS) null else statusForClock(summary.averageMinutes)
}
