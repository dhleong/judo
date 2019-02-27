package net.dhleong.judo.jline

import assertk.Assert
import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import assertk.assertions.support.show
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import net.dhleong.judo.SCROLL
import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.bufferOf
import net.dhleong.judo.emptyBuffer
import net.dhleong.judo.hasSize
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.SimpleFlavor
import net.dhleong.judo.render.toFlavorable
import net.dhleong.judo.util.ansi
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.junit.Before
import org.junit.Test

class JLineWindowTest {

    private lateinit var renderer: IJLineRenderer

    @Before fun setUp() {
        renderer = mock {  }
    }

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

    @Test fun `Blank line rendering`() {
        val display = JLineDisplay(10, 3)
        val buffer = bufferOf("""
            mreynolds

            zoe
        """.trimIndent())

        windowOf(buffer, 10, 3)
            .render(display, 0, 0)

        assert(display).linesEqual("""
            |mreynolds_
            |__________
            |zoe_______
        """.trimMargin())
    }

    @Test fun `Render long status line`() {
        val display = JLineDisplay(10, 3)
        val buffer = emptyBuffer()

        windowOf(buffer, 10, 3, focusable = true).apply {
            updateStatusLine(""":echo("mreynolds")""".toFlavorable(), 18)
            isFocused = true

            render(display, 0, 0)
        }

        assert(display).linesEqual("""
            |__________
            |__________
            |…nolds")__
        """.trimMargin())
    }

    @Test fun `Render with width-filled backgrounds`() {
        val display = JLineDisplay(10, 2)
        val buffer = bufferOf(
            FlavorableStringBuilder(10).apply {
                append("zoe ", SimpleFlavor(
                    hasBackground = true,
                    background = JudoColor.Simple.from(1)
                ))
                append("w", SimpleFlavor(
                    hasForeground = true,
                    foreground = JudoColor.Simple.from(2)
                ))
            },
            FlavorableStringBuilder(10).apply {
                append("wash", SimpleFlavor(
                    hasForeground = true,
                    foreground = JudoColor.Simple.from(1)
                ))
            }
        )

        windowOf(buffer, 10, 2)
            .render(display, 0, 0)

        assert(display).ansiLinesEqual("""
            |${ansi(bg = 1)}zoe_${ansi(fg = 2, bg = 9)}w_____${ansi(0)}
            |${ansi(fg = 1)}wash______${ansi(0)}
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

    @Test fun `Max scroll`() {
        val display = JLineDisplay(10, 3)
        val buffer = bufferOf("""
            captain
            mreynolds
            first
            mate
            zoe
        """.trimIndent())
        assert(buffer).hasSize(5)

        val w = windowOf(buffer, 10, 3, wrap = true)

        w.scrollLines(4)
        w.render(display, 0, 0)
        assert(w).hasScrollback(4)
        assert(display).linesEqual("""
            |__________
            |__________
            |captain___
        """.trimMargin())

        // now scroll back
        w.scrollLines(-4)
        w.render(display, 0, 0)
        assert(w).hasScrollback(0)
        assert(display).linesEqual("""
            |first_____
            |mate______
            |zoe_______
        """.trimMargin())
    }

    @Test fun `Scroll by setting`() {
        val display = JLineDisplay(10, 4)
        val buffer = bufferOf("""
            pilot
            wash
            captain
            mreynolds
            first
            mate
            zoe
        """.trimIndent())

        val state = StateMap()
        val w = windowOf(buffer, 10, 4, settings = state)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |mreynolds_
            |first_____
            |mate______
            |zoe_______
        """.trimMargin())

        // count is ignored; half window height is used
        w.scrollBySetting(4)
        w.render(display, 0, 0)
        assert(w).hasScrollback(2)
        assert(display).linesEqual("""
            |wash______
            |captain___
            |mreynolds_
            |first_____
        """.trimMargin())

        // when > 0, SCROLL is used
        state[SCROLL] = 1
        w.scrollBySetting(4)
        w.render(display, 0, 0)
        assert(w).hasScrollback(3)
        assert(display).linesEqual("""
            |pilot_____
            |wash______
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

        // don't allow scrolling further
        w.scrollLines(1)
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |__________
            |captain___
        """.trimMargin())
    }

    @Test fun `scrollToBottom clears offsets`() {
        val display = JLineDisplay(10, 3)
        val buffer = bufferOf("""
            Take my love, take my land,
            take me where I cannot stand
        """.trimIndent())
        val w = windowOf(buffer, 10, 3, wrap = true)

        w.scrollLines(2)
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |my land,__
            |take me___
            |where I___
        """.trimMargin())

        w.scrollToBottom()
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |where I___
            |cannot____
            |stand_____
        """.trimMargin())
    }

    @Test fun `Prevent scrolling behind end of buffer`() {
        val display = JLineDisplay(10, 3)
        val buffer = bufferOf("""
            captain
            reynolds
        """.trimIndent())
        assert(buffer).hasSize(2)

        val w = windowOf(buffer, 10, 3)
        w.scrollPages(1)
        assert(w).hasScrollback(1)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |__________
            |captain___
        """.trimMargin())

        // scroll back
        w.scrollPages(-1)
        assert(w).hasScrollback(0)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |captain___
            |reynolds__
        """.trimMargin())

        // prevent scrolling any further (at least one line of content on screen)
        w.scrollPages(-1)
        assert(w).hasScrollback(0)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |captain___
            |reynolds__
        """.trimMargin())
    }

    @Test fun `Prevent scrolling past visible buffer contents`() {
        val display = JLineDisplay(10, 3)
        val buffer = bufferOf("""
            captain
            reynolds
        """.trimIndent())
        assert(buffer).hasSize(2)

        val w = windowOf(buffer, 10, 3)
        assert(w).hasScrollback(0)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |captain___
            |reynolds__
        """.trimMargin())

        w.scrollPages(1)
        assert(w).hasScrollback(1)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |__________
            |captain___
        """.trimMargin())

        // prevent scrolling any further (at least one line of content on screen)
        w.scrollPages(1)
        assert(w).hasScrollback(1)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |__________
            |captain___
        """.trimMargin())
    }

    @Test fun `Prevent scrolling past visible buffer content when wrapped`() {
        val display = JLineDisplay(10, 3)
        val buffer = bufferOf("""
            captain reynolds
        """.trimIndent())

        val w = windowOf(buffer, 10, 3, wrap = true)
        assert(w).hasScrollback(0)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |captain___
            |reynolds__
        """.trimMargin())

        w.scrollPages(1)
        assert(w).hasScrollback(0)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |__________
            |captain___
        """.trimMargin())

        // prevent scrolling any further (at least one line of content on screen)
        w.scrollPages(1)
        assert(w).hasScrollback(0)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________
            |__________
            |captain___
        """.trimMargin())
    }

    @Test fun `scrollToLine in wrapped buffer`() {
        val display = JLineDisplay(10, 1)
        val buffer = bufferOf("""
            captain mreynolds
            first mate zoe
        """.trimIndent())
        val w = windowOf(buffer, 10, 1, wrap = true)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |zoe_______
        """.trimMargin())

        w.scrollToBufferLine(line = 1)
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |first mate
        """.trimMargin())

        w.scrollToBufferLine(line = 0, offsetOnLine = 0)
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |captain___
        """.trimMargin())

        w.scrollToBufferLine(line = 0, offsetOnLine = 8)
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |mreynolds_
        """.trimMargin())
    }

    @Test fun `Search in buffer`() {
        val display = JLineDisplay(12, 3)
        val buffer = bufferOf("""
            Take My love
            Take my land
            Take me where I cannot stand
        """.trimIndent())
        val w = windowOf(buffer, 12, 3, focused = true)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |e I cannot s
            |tand________
            |____________
        """.trimMargin())

        w.searchForKeyword("m", direction = 1)
        w.render(display, 0, 0)
        assert(display).ansiLinesEqual("""
            |Take my land
            |Take ${ansi(inverse = true)}m${ansi(0)}e wher
            |____________
        """.trimMargin())

        // NOTE: we avoid scrolling here since it's on the same page
        w.searchForKeyword("m", direction = 1)
        w.render(display, 0, 0)
        assert(display).ansiLinesEqual("""
            |Take ${ansi(inverse = true)}m${ansi(0)}y land
            |Take me wher
            |____________
        """.trimMargin())

        // step back
        w.searchForKeyword("m", direction = -1)
        w.render(display, 0, 0)
        assert(display).ansiLinesEqual("""
            |Take my land
            |Take ${ansi(inverse = true)}m${ansi(0)}e wher
            |____________
        """.trimMargin())

        // go to next page
        w.searchForKeyword("m", direction = 1)
        w.searchForKeyword("m", direction = 1)
        w.render(display, 0, 0)
        assert(display).ansiLinesEqual("""
            |____________
            |Take ${ansi(inverse = true)}M${ansi(0)}y love
            |____________
        """.trimMargin())
    }

    @Test fun `Search in un-focusable window always highlights`() {
        val display = JLineDisplay(12, 2)
        val buffer = bufferOf(
            """
            Take My love
            Take my land
            Take me where I cannot stand
        """.trimIndent())
        val w = windowOf(buffer, 12, 2, focusable = false)

        w.searchForKeyword("m", direction = 1)
        w.render(display, 0, 0)
        assert(display).ansiLinesEqual(
            """
            |Take my land
            |Take ${ansi(inverse = true)}m${ansi(0)}e wher
        """.trimMargin())
    }

    @Test fun `highlight search result in wrapped buffer`() {
        val display = JLineDisplay(10, 2)
        val buffer = bufferOf("""
            captain mreynolds
            first mate zoe
        """.trimIndent())
        val w = windowOf(buffer, 10, 2, wrap = true, focused = true)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |zoe_______
            |__________
        """.trimMargin())

        w.searchForKeyword("rey", 1)
        w.render(display, 0, 0)
        assert(w).hasScrollback(1)
        assert(display).linesEqual("""
            |mreynolds_
            |__________
        """.trimMargin())

        assert(display).ansiLinesEqual(
            buildAnsi {
                append("m")
                append("rey", AttributedStyle.INVERSE)
                append("nolds ")
                append("\n          ")
            }
        )
    }

    @Test fun `highlight correct search result of multiple on line`() {
        val display = JLineDisplay(26, 2)
        val buffer = bufferOf("""
            take my love, take my land
        """.trimIndent())
        val w = windowOf(buffer, 26, 2, focused = true)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            take my love, take my land
            __________________________
        """.trimIndent())

        w.searchForKeyword("e", 1)
        w.render(display, 0, 0)
        assert(display).ansiLinesEqual(
            buildAnsi {
                append("take my love, tak")
                append("e", AttributedStyle.INVERSE)
                append(" my land")
                append("\n                          ")
            }
        )

        w.searchForKeyword("e", 1)
        w.render(display, 0, 0)
        assert(display).ansiLinesEqual(
            buildAnsi {
                append("take my lov")
                append("e", AttributedStyle.INVERSE)
                append(", take my land")
                append("\n                          ")
            }
        )

        w.searchForKeyword("e", 1)
        w.render(display, 0, 0)
        assert(display).ansiLinesEqual(
            buildAnsi {
                append("tak")
                append("e", AttributedStyle.INVERSE)
                append(" my love, take my land")
                append("\n                          ")
            }
        )

        w.searchForKeyword("e", -1)
        w.render(display, 0, 0)
        assert(display).ansiLinesEqual(
            buildAnsi {
                append("take my lov")
                append("e", AttributedStyle.INVERSE)
                append(", take my land")
                append("\n                          ")
            }
        )
    }

    @Test fun `handle search with no result`() {
        val display = JLineDisplay(24, 1)
        val buffer = bufferOf("""
            |captain mreynolds_______
            |first mate zoe__________
        """.trimMargin())
        val w = windowOf(buffer, 24, 1, wrap = true)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |first mate zoe__________
        """.trimMargin())

        w.searchForKeyword("none", 1)
        w.render(display, 0, 0)

        // don't go anywhere
        assert(w).hasScrollback(0)
        assert(display).linesEqual("""
            |first mate zoe__________
        """.trimMargin())

        verify(renderer, times(1)).echo(eq(
            "Pattern not found: none".toFlavorable()
        ))
    }

    @Test fun `Maintain scrollback on append`() {
        val display = JLineDisplay(42, 2)
        val buffer = bufferOf("""
            |Take My love
            |Take my land
            |Take me where I cannot stand
        """.trimMargin())
        val w = windowOf(buffer, 42, 2)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |Take my land______________________________
            |Take me where I cannot stand______________
        """.trimMargin())

        // now resize the primary to force wrapping
        display.resize(6, 2)
        w.resize(6, 2)
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |nnot s
            |tand__
        """.trimMargin())

        w.scrollPages(1)
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |e wher
            |e I ca
        """.trimMargin())

        w.append(FlavorableStringBuilder.withDefaultFlavor("PAR"))
        w.append(FlavorableStringBuilder.withDefaultFlavor("TS\n"))
        w.appendLine("LINES")

        // since we're scrolled, we should stay
        // where we are
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |e wher
            |e I ca
        """.trimMargin())
    }

    @Test fun `Maintain scrollback in offset on append`() {
        val display = JLineDisplay(42, 2)
        val buffer = emptyBuffer().apply {
            append(FlavorableStringBuilder.withDefaultFlavor("Take me where"))
        }
        assert(buffer.size).isEqualTo(1)
        val w = windowOf(buffer, 42, 2)

        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |__________________________________________
            |Take me where_____________________________
        """.trimMargin())

        // now resize the primary to force wrapping
        display.resize(7, 2)
        w.resize(7, 2)
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |Take me
            | where_
        """.trimMargin())

        w.scrollLines(1)
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |_______
            |Take me
        """.trimMargin())

        w.append(FlavorableStringBuilder.withDefaultFlavor(" I cannot "))
        w.append(FlavorableStringBuilder.withDefaultFlavor("stand\n"))

        // since we're scrolled, we should stay
        // where we are
        w.render(display, 0, 0)
        assert(display).linesEqual("""
            |_______
            |Take me
        """.trimMargin())
    }

    @Test fun `Don't squash significant whitespace`() {
        // see #62
        val display = JLineDisplay(10, 1)
        val buffer = emptyBuffer().apply {
            append(FlavorableStringBuilder.withDefaultFlavor(".").apply {
                append("  ", SimpleFlavor(
                    hasForeground = true,
                    foreground = JudoColor.Simple.from(2)
                ))
                append("  ", SimpleFlavor(
                    hasForeground = true,
                    foreground = JudoColor.Simple.from(4)
                ))
            })
        }
        assert(buffer.size).isEqualTo(1)
        val w = windowOf(buffer, 10, 1)

        w.render(display, 0, 0)
        assert(display).ansiLinesEqual("""
            |.${ansi(fg=2)}__${ansi(fg=4)}_______${ansi(0)}
        """.trimMargin())
    }

    @Test fun `Render trailing Flavor`() {
        // see #62
        val display = JLineDisplay(10, 1)
        val buffer = emptyBuffer().apply {
            append(FlavorableStringBuilder.withDefaultFlavor(".").apply {
                append("  ", SimpleFlavor(
                    hasBackground = true,
                    background = JudoColor.Simple.from(2)
                ))
                trailingFlavor = SimpleFlavor(
                    hasBackground = true,
                    background = JudoColor.Simple.from(5)
                )
            })
        }
        assert(buffer.size).isEqualTo(1)
        val w = windowOf(buffer, 10, 1)

        w.render(display, 0, 0)
        assert(display).ansiLinesEqual("""
            |.${ansi(bg=2)}__${ansi(bg=5)}_______${ansi(0)}
        """.trimMargin())
    }

    private fun windowOf(
        buffer: JudoBuffer,
        width: Int,
        height: Int,
        focusable: Boolean = false,
        focused: Boolean = false,
        wrap: Boolean = false,
        settings: StateMap? = null
    ) = JLineWindow(
        renderer,
        IdManager(),
        settings ?: StateMap().apply {
            this[WORD_WRAP] = wrap
        },
        width,
        height,
        buffer,
        isFocusable = focused || focusable
    ).also {
        it.isFocused = focused
    }
}

private fun buildAnsi(block: AttributedStringBuilder.() -> Unit) =
    AttributedStringBuilder()
        .apply(block)
        .toAttributedString()
        .toAnsi()

private fun Assert<IJudoWindow>.hasScrollback(lines: Int) {
    if (actual.getScrollback() == lines) return
    expected("scrollback=${show(lines)} but was ${show(actual.getScrollback())}")
}

