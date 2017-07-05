package net.dhleong.judo.util

import java.util.EnumSet

/**
 * Abstraction over pattern matching functionality used by
 * Aliases, Triggers, and Prompts
 *
 * @author dhleong
 */

enum class PatternProcessingFlags {
    /**
     * By default, ANSI codes are stripped from
     * text matched before passing the matches to
     * the processor function; this flag can be used
     * to prevent that.
     */
    KEEP_COLOR;

    companion object {
        val NONE = EnumSet.noneOf(PatternProcessingFlags::class.java)
    }
}

interface PatternSpec {
    val flags: EnumSet<PatternProcessingFlags>

    val groups: Int
    fun matcher(input: CharSequence): PatternMatcher

    val original: String
}

interface PatternMatcher {
    fun find(): Boolean
    fun group(index: Int): String

    /** The start of the group at [index] */
    fun start(index: Int): Int
    /** The end of the group at [index] */
    fun end(index: Int): Int

    val start: Int
    val end: Int
}
