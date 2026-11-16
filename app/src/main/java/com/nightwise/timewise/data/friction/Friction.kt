package com.nightwise.timewise.data.friction

import kotlin.random.Random

/** A photo of someone the user wants to do better for. [locked] ones stay hidden until the PIN is entered. */
data class LovedPhoto(val id: String, val caption: String, val locked: Boolean)

data class Dream(val id: String, val text: String)

data class FrictionTodo(val id: String, val text: String, val done: Boolean)

/** An audio file picked from storage; [hasArt] when its embedded cover was saved. */
data class MotivationTrack(val id: String, val uri: String, val title: String, val artist: String?, val hasArt: Boolean)

/** Everything the user set up for the steps that stand between them and extra time. */
data class FrictionContent(
    val photos: List<LovedPhoto> = emptyList(),
    val dreams: List<Dream> = emptyList(),
    val todos: List<FrictionTodo> = emptyList(),
    val tracks: List<MotivationTrack> = emptyList(),
    /** Faith photos are always visible, so they never carry the lock. Up to [FrictionLogic.FAITH_MAX_PHOTOS]. */
    val faithPhotos: List<LovedPhoto> = emptyList(),
    val hasPin: Boolean = false
) {
    val openTodos: List<FrictionTodo> get() = todos.filter { !it.done }
}

/** In the order they're shown. */
enum class FrictionStep(val title: String, val builtIn: Boolean) {
    PHRASE("Type it out", builtIn = true),
    PEOPLE("Your people", builtIn = false),
    FAITH("Your faith", builtIn = false),
    DREAM("Your dreams", builtIn = false),
    AWARENESS("The real cost", builtIn = true),
    TODOS("Your to-dos", builtIn = false),
    MUSIC("Your music", builtIn = false)
}

object FrictionLogic {
    const val PEOPLE_SECONDS = 5
    const val FAITH_SECONDS = 5
    const val FAITH_MAX_PHOTOS = 5
    const val AWARENESS_SECONDS = 10
    const val MUSIC_SECONDS = 30

    val PHRASES = listOf(
        "I still want to watch the screen and make my life useless",
        "I am choosing my phone over my dreams right now",
        "These fifteen minutes will never come back to me",
        "I know more scrolling will not make me feel better",
        "I am trading my future for a few more videos",
        "My family and my goals can wait while I scroll",
        "I promised myself I would stop and I am breaking that promise",
        "Nothing I am about to watch will matter tomorrow",
        "I am letting an app decide how I spend my life",
        "I could be building my dream instead of watching others live theirs"
    )

    fun randomPhrase(random: Random = Random): String = PHRASES.random(random)

    /**
     * Steps the user goes through. Ones that need content are skipped until it's set up, unless
     * [all] is set: then every step is kept, and a step without content shows a written prompt instead.
     */
    fun steps(content: FrictionContent, all: Boolean = false): List<FrictionStep> =
        if (all) FrictionStep.entries else FrictionStep.entries.filter { isReady(it, content) }

    fun isReady(step: FrictionStep, content: FrictionContent): Boolean = when (step) {
        FrictionStep.PHRASE, FrictionStep.AWARENESS -> true
        FrictionStep.PEOPLE -> content.photos.isNotEmpty()
        FrictionStep.FAITH -> content.faithPhotos.isNotEmpty()
        FrictionStep.DREAM -> content.dreams.isNotEmpty()
        FrictionStep.TODOS -> content.openTodos.isNotEmpty()
        FrictionStep.MUSIC -> content.tracks.isNotEmpty()
    }

    /** A longer wait means a longer code: twice the minutes (5 min -> 10 characters, 10 -> 20...). */
    fun codeLength(cooldownMinutes: Int): Int = cooldownMinutes * 2

    // Look-alikes (0/O, 1/l/I) are left out so a code is never unfair to type.
    private const val CODE_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private const val CODE_DIGITS = "23456789"
    private const val CODE_SYMBOLS = "!@#$%&*?+=-"

    /** Random letters, digits and symbols, always with at least one digit and one symbol. */
    fun randomCode(length: Int, random: Random = Random): String {
        val size = length.coerceAtLeast(3)
        val pool = CODE_LETTERS + CODE_DIGITS + CODE_SYMBOLS
        val chars = mutableListOf(CODE_DIGITS.random(random), CODE_SYMBOLS.random(random))
        repeat(size - chars.size) { chars += pool.random(random) }
        return chars.shuffled(random).joinToString("")
    }

    /** Codes are case-sensitive; only stray spaces are forgiven. */
    fun matchesCode(typed: String, code: String): Boolean = typed.trim() == code

    /** The word to type next, and the text to fill in if the user taps it. */
    data class WordHint(val word: String, val typedAfter: String)

    /**
     * Completes the word being typed (or starts the next one) as long as nothing typed so far is wrong.
     * Null when the phrase is done or there's a typo to fix first.
     */
    fun nextWordHint(typed: String, phrase: String): WordHint? {
        if (hasTypo(typed, phrase) || matchesPhrase(typed, phrase)) return null
        val done = correctPrefixLength(typed, phrase)
        if (done >= phrase.length) return null
        val wordStart = if (done > 0 && phrase[done - 1] != ' ') phrase.lastIndexOf(' ', done - 1) + 1 else done
        val wordEnd = phrase.indexOf(' ', done).let { if (it < 0) phrase.length else it }
        val word = phrase.substring(wordStart, wordEnd)
        val after = phrase.substring(0, wordEnd) + if (wordEnd < phrase.length) " " else ""
        return WordHint(word, after)
    }

    /** Case, extra spaces and end punctuation don't matter; every word does. */
    fun normalize(text: String): String =
        text.trim().lowercase().replace(WHITESPACE, " ").trimEnd('.', '!', '?').trim()

    fun matchesPhrase(typed: String, phrase: String): Boolean = normalize(typed) == normalize(phrase)

    fun matchingDream(typed: String, dreams: List<Dream>): Dream? {
        val normalized = normalize(typed)
        if (normalized.isEmpty()) return null
        return dreams.firstOrNull { normalize(it.text) == normalized }
    }

    /** How much of [phrase] has been typed correctly from the start, for a progress hint. */
    fun correctPrefixLength(typed: String, phrase: String): Int {
        val a = loose(typed)
        val b = phrase.lowercase()
        var i = 0
        while (i < a.length && i < b.length && a[i] == b[i]) i++
        return i
    }

    /** True once something typed doesn't match the phrase so far. */
    fun hasTypo(typed: String, phrase: String): Boolean =
        !matchesPhrase(typed, phrase) && correctPrefixLength(typed, phrase) < loose(typed).length

    private fun loose(text: String): String = text.lowercase().replace(WHITESPACE, " ").trimStart()

    private val WHITESPACE = Regex("\\s+")
}
