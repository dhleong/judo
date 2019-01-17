package net.dhleong.judo.jline

import net.dhleong.judo.render.FlavorableStringBuilder
import org.junit.Test

class LineSplittingTest {
    private val s = FlavorableStringBuilder.withDefaultFlavor("Captain Mal Reynolds")

    @Test fun `Split lines ignores trailing new line`() {
        val shorter = FlavorableStringBuilder.withDefaultFlavor("Its Zoe\n")
        assertInWindow(width = 1, wordWrap = true) {
            shorter.hasRenderedLines(
                "I",
                "t",
                "s",
                "Z",
                "o",
                "e"
            )
        }
    }

    @Test fun `Split lines handles trailing whitespace`() {
        val shorter = FlavorableStringBuilder.withDefaultFlavor("Its Zoe  ")
        assertInWindow(width = 1, wordWrap = true) {
            shorter.hasRenderedLines(
                "I",
                "t",
                "s",
                "Z",
                "o",
                "e"
            )
        }
    }

    @Test fun `Split lines with word wrap and width 1`() {
        val shorter = FlavorableStringBuilder.withDefaultFlavor("Its Zoe")
        assertInWindow(width = 1, wordWrap = true) {
            shorter.hasRenderedLines(
                "I",
                "t",
                "s",
                "Z",
                "o",
                "e"
            )
        }
    }

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

    @Test fun `Split lines with word wrap right on whitespace`() {
        val s = FlavorableStringBuilder.withDefaultFlavor(
            "Press ENTER or type command to continue"
        )
        assertInWindow(width = 20, wordWrap = true) {
            s.hasRenderedLines(
                "Press ENTER or type",
                "command to continue"
            )
        }
    }

    @Test fun `Split preserving whitespace`() {
        val s = FlavorableStringBuilder.withDefaultFlavor(
            "The quick red fox jumped over the lazy brown dog. The quick red fox jumped"
        )
        assertInWindow(width = 20, wordWrap = true, preserveWhitespace = true) {
            s.hasRenderedLines(
                "The quick red fox ",
                "jumped over the lazy",
                " brown dog. The ",
                "quick red fox jumped"
            )
        }
    }
}
