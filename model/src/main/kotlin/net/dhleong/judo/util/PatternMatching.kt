package net.dhleong.judo.util

/**
 * Abstraction over pattern matching functionality used by
 * Aliases, Triggers, and Prompts
 *
 * @author dhleong
 */

interface PatternSpec {
    val groups: Int
    fun matcher(input: CharSequence): PatternMatcher

    val original: String
}

interface PatternMatcher {
    fun find(): Boolean
    fun group(index: Int): String

    val start: Int
    val end: Int
}
