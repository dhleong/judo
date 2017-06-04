package net.dhleong.judo.util

import org.jline.utils.AttributedString

/**
 * @author dhleong
 */

fun ansi(attr: Int = -1, fg: Int = -1, bg: Int = -1): CharSequence {
    val hasAttr = attr >= 0
    val hasFg = fg >= 0
    val hasBg = bg >= 0

    val builder = StringBuilder()
    builder.append(27.toChar())
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


fun stripAnsi(string: CharSequence): String =
    AttributedString.stripAnsi(string.toString())

fun stripAnsi(chars: CharArray, count: Int): CharSequence =
    AttributedString.stripAnsi(String(chars, 0, count))
