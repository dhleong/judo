package net.dhleong.judo.jline

import assertk.Assert
import assertk.assertions.isEqualTo

/**
 * Assert that the lines in the display match those in the
 * given string. Underscores may used in place of spaces
 * to visually indicate them.
 */
fun Assert<JLineDisplay>.linesEqual(expected: String) {
    val asString = actual.lines.map { it.toString() }
        .joinToString("\n")

        // allow using underscore to visually indicate spaces
        .replace(" ", "_")

    assert(asString).isEqualTo(
        expected.replace(" ", "_")
    )
}
