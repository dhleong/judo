package net.dhleong.judo.jline

import assertk.assert
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import net.dhleong.judo.MAX_INPUT_LINES
import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import org.junit.Before
import org.junit.Test

/**
 * This test was converted from the old `JLineRendererTest` so
 * it looks a little weird
 */
class TerminalInputLineHelperTest {

    private var windowWidth = 20
    private lateinit var settings: StateMap
    private lateinit var renderer: InputLine

    @Before fun setUp() {
        windowWidth = 20
        settings = StateMap(
            WORD_WRAP to false
        )
        renderer = InputLine()
    }

    @Test fun fitInputLineToWindow() {
        windowWidth = 12

        renderer.updateInputLine("Take my love, Take my land... ", 0)
        renderer.fitInputLineToWindow().let { (line, cursor) ->
            assertThat(line.toString()).isEqualTo("Take my lov…")
            assertThat(cursor).isEqualTo(0)
        }

        renderer.updateInputLine("Take my love, Take my land... ", 14)
        renderer.fitInputLineToWindow().let { (line, cursor) ->
            assertThat(line.toString()).isEqualTo("… love, Tak…")
            assertThat(cursor).isEqualTo(8)
        }

        renderer.updateInputLine("Take my love, Take my land... ", 19)
        renderer.fitInputLineToWindow().let { (line, cursor) ->
            assertThat(line.toString()).isEqualTo("… Take my l…")
            assertThat(cursor).isEqualTo(7)
        }

        renderer.updateInputLine("Take my love, Take my land... ", 30)
        renderer.fitInputLineToWindow().let { (line, cursor) ->
            assertThat(line.toString()).isEqualTo("…d... ")
            assertThat(cursor).isEqualTo(6)
        }
    }

    @Test fun fitInputLineToWindow_type() {
        windowWidth = 12

        renderer.typeAndFit("Take my love",
            expected = "… love" to 6)

        renderer.typeAndFit("Take my love,",
            expected = "… love," to 7)
    }

    @Test fun fitInputLineToWindow_type_page3() {
        windowWidth = 12

        renderer.typeAndFit("Take my love, take my lan",
            expected = "…my lan" to 7)

        renderer.typeAndFit("Take my love, take my land",
            expected = "…my land" to 8)

        renderer.typeAndFit("Take my love, take my land,",
            expected = "…my land," to 9)

        renderer.typeAndFit("Take my love, take my land, ",
            expected = "…my land, " to 10)

        renderer.typeAndFit("Take my love, take my land, t",
            expected = "…my land, t" to 11)

        renderer.typeAndFit("Take my love, take my land, ta",
            expected = "…d, ta" to 6)

        renderer.typeAndFit("Take my love, take my land, tak",
            expected = "…d, tak" to 7)
    }


    @Test fun fitInputLinesToWindow() {
        windowWidth = 12
        settings[MAX_INPUT_LINES] = 2

        renderer.updateInputLine("Take my love, Take my land... ", 0)
        renderer.fitInputLinesToWindow().let { (lines, cursor) ->
            assertThat(lines.map { it.toString() })
                .containsExactly("Take my love", ", Take my l…")
            assertThat(cursor).isEqualTo(0 to 0)
        }

        renderer.updateInputLine("Take my love, Take my land... ", 14)
        renderer.fitInputLinesToWindow().let { (lines, cursor) ->
            assertThat(lines.map { it.toString() })
                .containsExactly("Take my love", ", Take my l…")
            assertThat(cursor).isEqualTo(1 to 2)
        }

        renderer.updateInputLine("Take my love, Take my land... ", 19)
        renderer.fitInputLinesToWindow().let { (lines, cursor) ->
            assertThat(lines.map { it.toString() })
                .containsExactly("Take my love", ", Take my l…")
            assertThat(cursor).isEqualTo(1 to 7)
        }

        renderer.updateInputLine("Take my love, Take my land... ", 30)
        renderer.fitInputLinesToWindow().let { (lines, cursor) ->
            assertThat(lines.map { it.toString() })
                .containsExactly("… Take my la", "nd... ")
            assertThat(cursor).isEqualTo(1 to 6)
        }
    }

    @Test fun fitInputLinesToWindow_type() {
        windowWidth = 12
        settings[MAX_INPUT_LINES] = 2

        renderer.typeMultiAndFit("Take my love, Take my",
            expected = listOf("Take my love", ", Take my") to (1 to 9))

        renderer.typeMultiAndFit("Take my    l",
            expected = listOf("Take my    l", "") to (1 to 0))
    }

    @Test fun fitInputLinesToWindow2() {
        windowWidth = 20
        settings[MAX_INPUT_LINES] = 4
        settings[WORD_WRAP] = true

        val text = "The quick red fox jumped over the lazy brown dog. The quick red fox jumped"
        renderer.typeMultiAndFit(text,
            expected = listOf(
                "…umped over the lazy",
                " brown dog. The ",
                "quick red fox jumped",
                ""
            ) to (3 to 0))

        renderer.updateInputLine(text, 0)
        renderer.fitInputLinesToWindow().let { (lines, cursor) ->
            assertThat(lines.map { it.toString() })
                .containsExactly(
                    "The quick red fox ",
                    "jumped over the lazy",
                    " brown dog. The ",
                    "quick red fox jumped")
            assertThat(cursor).isEqualTo(0 to 0)
        }
    }

    @Test fun `fit with word wrap and space right at width`() {
        windowWidth = 10
        settings[MAX_INPUT_LINES] = 4
        settings[WORD_WRAP] = true

        val text = "The quick "
        renderer.typeMultiAndFit(text,
            expected = listOf(
                "The quick ",
                ""
            ) to (1 to 0))
    }

    private fun InputLine.updateInputLine(text: String, cursor: Int) {
        line = FlavorableStringBuilder.withDefaultFlavor(text)
        cursorIndex = cursor
    }

    private fun InputLine.fitInputLineToWindow(): Pair<FlavorableCharSequence, Int> {
        val lines = mutableListOf<FlavorableCharSequence>()
        TerminalInputLineHelper(settings, windowWidth)
            .fitInputLinesToWindow(this, lines)
        return lines[0] to cursorCol
    }
    private fun InputLine.fitInputLinesToWindow(): Pair<List<FlavorableCharSequence>, Pair<Int, Int>> {
        val lines = mutableListOf<FlavorableCharSequence>()
        TerminalInputLineHelper(settings, windowWidth).fitInputLinesToWindow(this, lines)
        return lines to (cursorRow to cursorCol)
    }

    private fun InputLine.typeAndFit(text: String, expected: Pair<String, Int>) {
        updateInputLine(text, text.length)

        val (line, cursor) = fitInputLineToWindow()

        assertThat(line.toString() to cursor).isEqualTo(expected)
    }

    private fun InputLine.typeMultiAndFit(text: String, expected: Pair<List<String>, Pair<Int, Int>>) {
        updateInputLine(text, text.length)

        val (lines, cursor) = fitInputLinesToWindow()

        assertThat(lines.map { it.toString() } to cursor)
            .isEqualTo(expected)
    }
}

