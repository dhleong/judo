package net.dhleong.judo.jline

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.jline.utils.AttributedCharSequence
import org.jline.utils.AttributedStyle

/**
 * Assert that the lines in the display match those in the
 * given string. Underscores may used in place of spaces
 * to visually indicate them.
 */
fun Assert<JLineDisplay>.linesEqual(expected: String) {
    val asString = actual.lines.take(actual.height)
        .map { it.toString() }
        .joinToString("\n")

        // allow using underscore to visually indicate spaces
        .replace(" ", "_")

    assert(asString).isEqualTo(
        expected.replace(" ", "_")
    )
}

fun Assert<JLineDisplay>.ansiLinesEqual(expected: String) {
    val asString = actual.lines.take(actual.height)
        .joinToString("\n") { it.toAnsi() }

        // allow using underscore to visually indicate spaces
        .replace(" ", "_")

    assert(asString).isEqualTo(
        expected.replace(" ", "_")
    )
}

fun Assert<JLineDisplay>.hasCursor(row: Int, col: Int) {
    if (actual.cursorRow == row && actual.cursorCol == col) return
    expected("cursor to be at ${show(row to col)} but was ${show(
        actual.cursorRow to actual.cursorCol
    )}")
}

fun Assert<AttributedCharSequence>.hasStyleAt(i: Int, style: AttributedStyle) {
    val actualStyle = actual.styleAt(i)
    if (actualStyle == style) return
    expected("style at ${show(i)} = ${show(style)} but was ${show(actualStyle)}")
}
