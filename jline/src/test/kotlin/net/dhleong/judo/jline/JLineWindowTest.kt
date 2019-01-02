package net.dhleong.judo.jline

import assertk.Assert
import assertk.assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.render.*
import org.junit.Test

class JLineWindowTest {

    @Test fun `Simple Window rendering`() {
        val display = JLineDisplay(10, 3)
        val buffer = bufferOf("""
            mreynolds
            zoe
        """.trimIndent())

        windowOf(buffer, 10, 3)
            .render(display, 0, 0)

        assert(display).linesEqual("""
            |__________
            |mreynolds_
            |zoe_______
        """.trimMargin())
    }

    @Test fun `Render empty buffer`() {
        val display = JLineDisplay(10, 3)
        val buffer = bufferOf("")

        windowOf(buffer, 10, 3)
            .render(display, 0, 0)

        assert(display).linesEqual("""
            |__________
            |_        _
            |__________
        """.trimMargin())
    }

    @Test fun `Hard buffer line breaks`() {
        val display = JLineDisplay(10, 3)
        val buffer = bufferOf("""
            captain mreynolds
        """.trimIndent())

        windowOf(buffer, 10, 3)
            .render(display, 0, 0)

        assert(display).linesEqual("""
            |__________
            |captain mr
            |eynolds___
        """.trimMargin())
    }

    @Test fun `Word wrap buffers`() {
        val display = JLineDisplay(10, 3)
        val buffer = bufferOf("""
            captain mreynolds
        """.trimIndent())

        windowOf(buffer, 10, 3, wrap = true)
            .render(display, 0, 0)

        assert(display).linesEqual("""
            |__________
            |captain___
            |mreynolds_
        """.trimMargin())
    }

    @Test fun `Scrolling in wrapped buffers`() {
        val display = JLineDisplay(10, 3)
        val buffer = bufferOf("""
            captain mreynolds
            first mate zoe
        """.trimIndent())
        val w = windowOf(buffer, 10, 3, wrap = true)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |mreynolds_
            |first mate
            |zoe_______
        """.trimMargin())

        w.scrollLines(1)
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |captain___
            |mreynolds_
            |first mate
        """.trimMargin())

        w.scrollLines(1)
        assert(w).hasScrollback(1) // should be at line 1 now
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |captain___
            |mreynolds_
        """.trimMargin())

        w.scrollLines(1)
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |__________
            |captain___
        """.trimMargin())
    }
}

private fun Assert<IJudoWindow2>.hasScrollback(lines: Int) {
    if (actual.getScrollback() == lines) return
    expected("scrollback=${show(lines)} but was ${show(actual.getScrollback())}")
}

private fun bufferOf(contents: String) = JudoBuffer2(IdManager()).apply {
    contents.split("\n").forEach {
        appendLine(FlavorableStringBuilder.fromString(it).apply {
            beginFlavor(Flavor.default, 0)
        })
    }
}

private fun windowOf(
    buffer: JudoBuffer2,
    width: Int,
    height: Int,
    focusable: Boolean = false,
    wrap: Boolean = false
) = JLineWindow(
    IdManager(),
    StateMap().apply {
        this[WORD_WRAP] = wrap
    },
    width,
    height,
    buffer,
    isFocusable = focusable
)
