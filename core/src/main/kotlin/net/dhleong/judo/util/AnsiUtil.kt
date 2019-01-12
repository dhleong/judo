package net.dhleong.judo.util

/**
 * @author dhleong
 */

const val ESCAPE_CODE_SEARCH_LIMIT = 10
const val ESCAPE_CHAR = 27.toChar()

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

    if (hasBg) {
        builder.append(40 + bg)
    }

    builder.append('m')
    return builder
}

fun ansi(inverse: Boolean): CharSequence =
    if (inverse) "$ESCAPE_CHAR[7m"
    else "$ESCAPE_CHAR[27m"

