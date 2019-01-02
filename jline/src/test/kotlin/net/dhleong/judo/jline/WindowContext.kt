package net.dhleong.judo.jline

import assertk.assert
import assertk.assertions.isEqualTo
import net.dhleong.judo.render.FlavorableStringBuilder

/**
 * Convenience for writing assertions on [FlavorableStringBuilder] that
 * share the context of a Window
 */
class WindowContext(
    private val windowWidth: Int,
    private val wordWrap: Boolean
) {
    fun FlavorableStringBuilder.hasRenderedLinesCount(count: Int) {
        assert(
            computeRenderedLinesCount(windowWidth, wordWrap),
            describe("rendered lines count")
        ).isEqualTo(count)
    }

    fun FlavorableStringBuilder.hasRenderedLines(vararg lines: String) {
        val actual = mutableListOf<String>()
        forEachRenderedLine(windowWidth, wordWrap) { start, end ->
            actual += substring(start, end)
        }
        assert(
            actual,
            describe("rendered lines")
        ).isEqualTo(lines.toList())
    }

    private fun describe(what: String) =
        "In (width=$windowWidth, wrap=$wordWrap): $what of $this"
}

fun assertInWindow(
    width: Int,
    wordWrap: Boolean,
    block: WindowContext.() -> Unit
) = WindowContext(width, wordWrap = wordWrap).block()
