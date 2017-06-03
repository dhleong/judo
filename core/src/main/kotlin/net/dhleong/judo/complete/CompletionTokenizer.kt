package net.dhleong.judo.complete

import java.util.regex.Pattern

/**
 * @author dhleong
 */

private val tokenRegex = Pattern.compile("(\\w{3,}([']\\w+)?)")

/**
 * @return a lazy Sequence of completable Strings
 */
fun tokensFrom(string: CharSequence): Sequence<String> {
    val matcher = tokenRegex.matcher(string)
    return generateSequence {
        if (matcher.find()) {
            matcher.group()
        } else {
            null
        }
    }
}
