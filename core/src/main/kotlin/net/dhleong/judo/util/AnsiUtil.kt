package net.dhleong.judo.util

import org.jline.utils.AttributedString

/**
 * @author dhleong
 */

fun ansi(attr: Int, fg: Int, bg: Int = -1): CharSequence {
    val builder = StringBuilder()
    builder.append(27.toChar())
        .append('[')
        .append(attr)
        .append(';')
        .append(30 + fg)
    if (bg >= 0) {
        builder.append(';')
            .append(40 + bg)
    }
    builder.append('m')
    return builder
}


fun stripAnsi(string: CharSequence): String =
    AttributedString.stripAnsi(string.toString())

fun stripAnsi(chars: CharArray, count: Int): CharSequence =
    AttributedString.stripAnsi(String(chars, 0, count))
