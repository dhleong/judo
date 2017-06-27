package net.dhleong.judo.complete

import java.util.regex.Pattern

/**
 * @author dhleong
 */

private val tokenRegex = Pattern.compile("(\\w{3,}([']\\w+)?)")
private val tokenRegexAnyLength = Pattern.compile("(\\w+([']\\w+)?)")

/**
 * @return a lazy Sequence of completable Strings
 */
fun tokensFrom(string: CharSequence, anyLength: Boolean = false): Sequence<String> {
    val regex =
        if (anyLength) tokenRegexAnyLength
        else tokenRegex
    val matcher = regex.matcher(string)
    return generateSequence {
        if (matcher.find()) {
            matcher.group()
        } else {
            null
        }
    }
}
