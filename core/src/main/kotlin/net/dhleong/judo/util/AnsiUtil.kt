package net.dhleong.judo.util

import org.jline.utils.AttributedCharSequence
import org.jline.utils.AttributedString

/**
 * @author dhleong
 */

val ESCAPE_CODE_SEARCH_LIMIT = 8
val ESCAPE_CHAR = 27.toChar()

fun ansi(attr: Int = -1, fg: Int = -1, bg: Int = -1): CharSequence {
    val hasAttr = attr >= 0
    val hasFg = fg >= 0
    val hasBg = bg >= 0

    val builder = StringBuilder()
    builder.append(ESCAPE_CHAR)
        .append('[')
    if (hasAttr) {
        builder.append(attr)

        if (hasFg || hasBg) builder.append(';')
    }

    if (hasFg) {
        builder.append(30 + fg)

        if (hasBg) {
            builder.append(';')
        }
    }

    if (bg >= 0) {
        builder.append(40 + bg)
    }

    builder.append('m')
    return builder
}

fun findTrailingEscape(chars: CharSequence): CharSequence? {
    val lastIndex = chars.lastIndex
    if (lastIndex == -1) return null
    if (chars[lastIndex] != 'm') {
        // quick shortcut; if it doesn't end with m, it doesn't
        // end with a full escape; either it's split, or there's
        // just more text
        return null
    }

    val start = maxOf(0, lastIndex - ESCAPE_CODE_SEARCH_LIMIT)

    return (lastIndex-1 downTo start)
        .firstOrNull { chars[it] == ESCAPE_CHAR }
        ?.let { chars.subSequence(it..lastIndex) }
}

fun stripAnsi(string: CharSequence): String =
    (string as? AttributedCharSequence)?.toString() // ansi is already stripped
        ?: AttributedString.stripAnsi(string.toString())

