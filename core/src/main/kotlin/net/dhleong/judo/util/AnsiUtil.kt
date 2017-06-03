package net.dhleong.judo.util

import org.jline.utils.AttributedString

/**
 * @author dhleong
 */

fun stripAnsi(chars: CharArray, count: Int): CharSequence =
    AttributedString.stripAnsi(String(chars, 0, count))
