package net.dhleong.judo.jline

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.jline.utils.AttributedCharSequence
import org.jline.utils.AttributedStyle
import org.junit.ComparisonFailure

/**
 * Assert that the lines in the display match those in the
 * given string. Underscores may used in place of spaces
 * to visually indicate them.
 */
fun Assert<JLineDisplay>.linesEqual(expected: String) = given { actual ->
    val asString = actual.lines.take(actual.height)
        .joinToString("\n") { it.toString() }

        // allow using underscore to visually indicate spaces
        .replace(" ", "_")

    val expectedClean = expected.replace(" ", "_")
    if (asString == expectedClean) return

    // using junit's ComparisonFailure seems to enable intellij to let us
    // see a nice side-by-side, like assertj used to give us
    throw ComparisonFailure(
        "",
        expectedClean,
        asString
    )
}

fun Assert<JLineDisplay>.ansiLinesEqual(expected: String) = given { actual ->
    val asString = actual.lines.take(actual.height)
        .joinToString("\n") { it.toAnsi() }

        // allow using underscore to visually indicate spaces
        .replace(" ", "_")

    assertThat(asString).isEqualTo(
        expected.replace(" ", "_")
    )
}

fun Assert<JLineDisplay>.hasCursor(row: Int, col: Int) = given { actual ->
    if (actual.cursorRow == row && actual.cursorCol == col) return
    expected("cursor to be at ${show(row to col)} but was ${show(
        actual.cursorRow to actual.cursorCol
    )}")
}

fun Assert<AttributedCharSequence>.hasStyleAt(i: Int, style: AttributedStyle) = given { actual ->
    val actualStyle = actual.styleAt(i)
    if (actualStyle == style) return
    expected("style at ${show(i)} = ${show(style)} but was ${show(actualStyle)}")
}
