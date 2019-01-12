package net.dhleong.judo.jline

import assertk.assert
import assertk.assertions.isInstanceOf
import org.junit.Test

class JLineDisplayTest {
    @Test fun `Empty display`() {
        val display = JLineDisplay(11, 3)
        assert(display).linesEqual("""
            |___________
            |_         _
            |___________
        """.trimMargin())
    }

    @Test fun `Write into Display`() {
        val display = JLineDisplay(11, 3)
        display.withLine(2, 1) {
            append("mreynolds")
        }

        assert(display).linesEqual("""
            |___________
            |  mreynolds
            |___________
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

