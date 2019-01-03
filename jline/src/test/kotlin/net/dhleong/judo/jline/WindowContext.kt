package net.dhleong.judo.jline

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.support.show
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder

/**
 * Convenience for writing assertions on [FlavorableStringBuilder] that
 * share the context of a Window
 */
class WindowContext(
    private val windowWidth: Int,
    private val wordWrap: Boolean,
    private val preserveWhitespace: Boolean
) {
    fun FlavorableStringBuilder.hasRenderedLinesCount(count: Int) {
        assert(
            computeRenderedLinesCount(windowWidth, wordWrap),
            describe("rendered lines count")
        ).isEqualTo(count)
    }

    fun FlavorableStringBuilder.hasRenderedLines(vararg lines: String) {
        val actual = mutableListOf<String>()
        forEachRenderedLine(windowWidth, wordWrap, preserveWhitespace) { start, end ->
            actual += substring(start, end)
        }
        assert(
            actual,
            describe("rendered lines")
        ).isEqualTo(lines.toList())
    }

    private fun FlavorableCharSequence.describe(what: String) =
        "In (\nwidth=$windowWidth,\n wrap=$wordWrap,\nwhite=$preserveWhitespace\n): $what of ${show(this)}\n"
}

fun assertInWindow(
    width: Int,
    wordWrap: Boolean,
    preserveWhitespace: Boolean = false,
    block: WindowContext.() -> Unit
) = WindowContext(
    width,
    wordWrap = wordWrap,
    preserveWhitespace = preserveWhitespace
).block()
