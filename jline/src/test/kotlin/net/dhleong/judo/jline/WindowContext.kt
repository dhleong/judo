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
            computeRenderedLines(windowWidth, wordWrap),
            "In (width=$windowWidth, wrap=$wordWrap): rendered lines count of $this"
        ).isEqualTo(count)
    }

    fun FlavorableStringBuilder.hasRenderedLines(vararg lines: String) {
        val actual = mutableListOf<String>()
        forEachRenderedLine(windowWidth, wordWrap) { start, end ->
            actual += substring(start, end)
        }
        assert(
            actual,
            "In (width=$windowWidth, wrap=$wordWrap): rendered lines of $this"
        ).isEqualTo(lines.toList())
    }
}

fun assertInWindow(
    width: Int,
    wordWrap: Boolean,
    block: WindowContext.() -> Unit
) = WindowContext(width, wordWrap = wordWrap).block()
