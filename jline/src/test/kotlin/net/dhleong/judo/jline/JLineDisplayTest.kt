package net.dhleong.judo.jline

import assertk.Assert
import assertk.assert
import assertk.assertions.*
import org.junit.Test

class JLineDisplayTest {
    @Test fun `Write into Display`() {
        val display = JLineDisplay(11, 3)
        display.withLine(2, 1) {
            append("mreynolds")
        }

        assert(display).linesEqual("""
            |___________
            |  mreynolds
        """.trimMargin())
    }

    @Test fun `Prevent writing out of range`() {
        val display = JLineDisplay(10, 3)
        assert {
            display.withLine(14, 1) {
                // nop
            }
        }.thrownError {
            isInstanceOf(IndexOutOfBoundsException::class.java)
        }

        assert {
            display.withLine(0, 20) {
                // nop
            }
        }.thrownError {
            isInstanceOf(IndexOutOfBoundsException::class.java)
        }

        assert {
            display.withLine(4, 1) {
                append("mreynolds")
            }
        }.thrownError {
            isInstanceOf(IndexOutOfBoundsException::class.java)
        }
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
