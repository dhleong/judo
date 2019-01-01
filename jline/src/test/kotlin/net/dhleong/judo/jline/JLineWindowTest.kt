package net.dhleong.judo.jline

import assertk.assert
import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.render.Flavor
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer2
import org.junit.Ignore
import org.junit.Test

class JLineWindowTest {

    @Ignore("TODO")
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

    @Ignore
    @Test fun `Word wrap buffers`() { TODO() }

    @Ignore
    @Test fun `Scrolling in wrapped buffers`() { TODO() }
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
