package net.dhleong.judo.jline

import net.dhleong.judo.render.FlavorableStringBuilder
import org.junit.Ignore
import org.junit.Test

class LineSplittingTest {
    private val s = FlavorableStringBuilder.withDefaultFlavor("Captain Mal Reynolds")

    @Ignore("FIXME")
    @Test fun `Split lines with word wrap and width 3`() {
        assertInWindow(width = 3, wordWrap = true) {
            s.hasRenderedLines(
                "Cap",
                "tai",
                "n",
                "Mal",
                "Rey",
                "nol",
                "ds"
            )
        }
    }

    @Test fun `Split lines with word wrap and width 4`() {
        assertInWindow(width = 4, wordWrap = true) {
            s.hasRenderedLines(
                "Capt",
                "ain",
                "Mal",
                "Reyn",
                "olds"
            )
        }
    }

    @Test fun `Split lines with word wrap and width 5`() {
        assertInWindow(width = 5, wordWrap = true) {
            s.hasRenderedLines(
                "Capta",
                "in",
                "Mal",
                "Reyno",
                "lds"
            )
        }
    }

    @Test fun `Split lines with word wrap and width 7`() {
        assertInWindow(width = 7, wordWrap = true) {
            s.hasRenderedLines(
                "Captain",
                "Mal",
                "Reynold",
                "s"
            )
        }
    }

    @Test fun `Split lines with word wrap and width 8`() {
        assertInWindow(width = 8, wordWrap = true) {
            s.hasRenderedLines(
                "Captain",
                "Mal",
                "Reynolds"
            )
        }
    }

    @Test fun `Split lines with word wrap and width 11`() {
        assertInWindow(width = 11, wordWrap = true) {
            s.hasRenderedLines(
                "Captain Mal",
                "Reynolds"
            )
        }
    }
}
