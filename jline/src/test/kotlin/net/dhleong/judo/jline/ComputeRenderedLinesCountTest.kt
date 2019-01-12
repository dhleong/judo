package net.dhleong.judo.jline

import net.dhleong.judo.render.FlavorableStringBuilder
import org.junit.Test

class ComputeRenderedLinesCountTest {
    @Test fun `No-wrap lines`() {
        val s = FlavorableStringBuilder.withDefaultFlavor("mreynolds")
        assertInWindow(width = 1, wordWrap = false) {
            s.hasRenderedLinesCount(9)
        }
        assertInWindow(width = 2, wordWrap = false) {
            s.hasRenderedLinesCount(5)
        }
        assertInWindow(width = 3, wordWrap = false) {
            s.hasRenderedLinesCount(3)
        }
        assertInWindow(width = 4, wordWrap = false) {
            s.hasRenderedLinesCount(3)
        }
        assertInWindow(width = 5, wordWrap = false) {
            s.hasRenderedLinesCount(2)
        }
        assertInWindow(width = 8, wordWrap = false) {
            s.hasRenderedLinesCount(2)
        }
        assertInWindow(width = 9, wordWrap = false) {
            s.hasRenderedLinesCount(1)
        }
    }

    @Test fun `Word-wrapped lines`() {
        val s = FlavorableStringBuilder.withDefaultFlavor("Captain Mal Reynolds")
        assertInWindow(width = 1, wordWrap = true) {
            s.hasRenderedLinesCount(18) // no spaces!
        }
        assertInWindow(width = 2, wordWrap = true) {
            s.hasRenderedLinesCount(10)
        }
        assertInWindow(width = 3, wordWrap = true) {
            s.hasRenderedLinesCount(7)
        }
        assertInWindow(width = 4, wordWrap = true) {
            s.hasRenderedLinesCount(5)
        }
        assertInWindow(width = 5, wordWrap = true) {
            s.hasRenderedLinesCount(5)
        }
        assertInWindow(width = 7, wordWrap = true) {
            s.hasRenderedLinesCount(4)
        }
        assertInWindow(width = 8, wordWrap = true) {
            s.hasRenderedLinesCount(3)
        }
        assertInWindow(width = 11, wordWrap = true) {
            s.hasRenderedLinesCount(2)
        }
    }
}

