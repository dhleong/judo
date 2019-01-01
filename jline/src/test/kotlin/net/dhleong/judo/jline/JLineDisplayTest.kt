package net.dhleong.judo.jline

import assertk.Assert
import assertk.assert
import assertk.assertions.isEqualTo
import org.junit.Test

class JLineDisplayTest {
    @Test fun `Write into Display`() {
        val display = JLineDisplay(11)
        display.withLine(2, 1) {
            append("mreynolds")
        }

        assert(display).linesEqual("""
            |___________
            |  mreynolds
        """.trimMargin())
    }
}

private fun Assert<JLineDisplay>.linesEqual(expected: String) {
    val asString = actual.lines.map { it.toString() }
        .joinToString("\n")
    assert(asString).isEqualTo(
        // allow using underscore to visually indicate spaces
        expected.replace("_", " ")
    )
}
