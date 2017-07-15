package net.dhleong.judo

import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.render.JudoTabpage
import net.dhleong.judo.render.OutputLine
import net.dhleong.judo.render.PrimaryJudoWindow
import net.dhleong.judo.util.ansi
import org.assertj.core.api.Assertions.assertThat
import org.jline.utils.AttributedString
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * TODO: A lot of these are more like JudoCore+JudoWindow/JudoBuffer integration tests
 * But since JudoCore depends on a JudoRenderer, we've put them here for now....
 * @author dhleong
 */
class JLineRendererTest {

    lateinit var renderer: JLineRenderer
    lateinit var outputBuffer: JudoBuffer
    lateinit var window: PrimaryJudoWindow
    lateinit var page: IJudoTabpage

    val settings = StateMap(
        WORD_WRAP to false
    )

    @Before fun setUp() {
        val ids = IdManager()
        outputBuffer = JudoBuffer(ids)

        window = PrimaryJudoWindow(ids, settings, outputBuffer, 20, 5)
        window.isFocused = true
        page = JudoTabpage(ids, settings, window)

        renderer = JLineRenderer(settings, false)
        renderer.windowWidth = 20
        renderer.windowHeight = 6
        renderer.currentTabpage = page
        renderer.updateSize()
    }

    @After fun tearDown() {
        renderer.close()
    }

    @Test fun basicRender() {
        window.appendLine("Take my love", isPartialLine = false)
        window.appendLine("Take my land", isPartialLine = false)
        window.updateStatusLine("<status>")
        renderer.updateInputLine("Test", 4)

        val (lines, cursorRow, cursorCol) = renderer.getDisplayLines()
        assertThat(lines.map { it.toString() })
            .containsExactly(
                "",
                "",
                "Take my love",
                "Take my land",
                "<status>",
                "Test"
            )

        assertThat(cursorRow).isEqualTo(5)
        assertThat(cursorCol).isEqualTo(4)
    }

    @Test fun appendOutput_resumePartial_splitAnsi_integration() {
        // we've tested the core handling with JudoBufferTest and JudoWindowTest,
        // so let's make sure JudoCore integrates into it correctly
        val core = JudoCore(renderer, settings)

        val ansi = ansi(fg=2)
        val firstHalf = ansi.slice(0..ansi.lastIndex-1)
        val secondHalf = ansi.slice(ansi.lastIndex..ansi.lastIndex)
        assertThat("$firstHalf$secondHalf").isEqualTo(ansi.toString())
        assertThat("$secondHalf").isEqualTo("m")

        val lineOne = "${ansi(1,1)}Take my $firstHalf"
        val lineTwo = "${secondHalf}l${ansi(1,6)}ove"

        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)

        val buffer = core.renderer.currentTabpage!!.currentWindow.currentBuffer
        assertThat(buffer.size)
            .overridingErrorMessage("Expected a non-empty output")
            .isGreaterThan(0)
        if (buffer[0].startsWith("ERROR")) {
            throw AssertionError(buffer.getAnsiContents().joinToString("\n"))
        }

        assertThat(buffer.getAnsiContents()[0])
            .isEqualTo("${ansi(1,1)}Take my ${ansi(fg=2)}l${ansi(fg=6)}ove${ansi(0)}")
    }

    @Test fun fitInputLineToWindow() {
        renderer.windowWidth = 12

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
        renderer.windowWidth = 12

        renderer.typeAndFit("Take my love",
            expected = "… love" to 6)

        renderer.typeAndFit("Take my love,",
            expected = "… love," to 7)
    }

    @Test fun fitInputLineToWindow_type_page3() {
        renderer.windowWidth = 12

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
        renderer.windowWidth = 12
        renderer.settings[MAX_INPUT_LINES] = 2

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
        renderer.windowWidth = 12
        renderer.settings[MAX_INPUT_LINES] = 2

        renderer.typeMultiAndFit("Take my love, Take my",
            expected = listOf("Take my love", ", Take my") to (1 to 9))

        renderer.typeMultiAndFit("Take my    l",
            expected = listOf("Take my    l", "") to (1 to 0))
    }

    @Test fun fitInputLinesToWindow2() {
        renderer.windowWidth = 20
        renderer.windowHeight = 6
        renderer.settings[MAX_INPUT_LINES] = 4
        renderer.settings[WORD_WRAP] = true

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

    @Test fun catchSplitPrompts() {
        val judo = JudoCore(renderer, settings)
        judo.prompts.define("HP: $1", "(hp: $1)")

        val prompt = "${ansi(1,3)}HP: ${ansi(1,6)}42\r\n"
        val first = prompt.substring(0..8)
        val second = prompt.substring(9..prompt.lastIndex)
        assertThat("$first$second").isEqualTo(prompt)

        judo.onIncomingBuffer(first.toCharArray(), first.length)
        judo.onIncomingBuffer(second.toCharArray(), second.length)

        val buffer = judo.renderer.currentTabpage!!.currentWindow.currentBuffer
        assertThat(buffer.getAnsiContents())
            .containsExactly("")
    }

    @Test fun catchSplitPrompts_splitAnsi() {

        val judo = JudoCore(renderer, settings)
        judo.prompts.define("HP: $1", "(hp: $1)")

        val prompt = "${ansi(1,3)}HP: ${ansi(1,6)}42\r\n"
        val first = prompt.substring(0..5)
        val second = prompt.substring(6..prompt.lastIndex)
        assertThat("$first$second").isEqualTo(prompt)

        judo.onIncomingBuffer(first.toCharArray(), first.length)
        judo.onIncomingBuffer(second.toCharArray(), second.length)

        val buffer = judo.renderer.currentTabpage!!.currentWindow.currentBuffer
        assertThat(buffer.getAnsiContents())
            .containsExactly("")
    }

    @Test fun continueStyleAcrossLines() {
        val core = JudoCore(renderer, settings)

        val lineOne = "${ansi(1,1)}Take my love,\r\n"
        val lineTwo = "Take my land...\r\n"

        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)

        val buffer = core.renderer.currentTabpage!!.currentWindow.currentBuffer
        if (buffer[0].startsWith("ERROR")) {
            throw AssertionError(buffer.getAnsiContents().joinToString("\n"))
        }

        val bufferContents = buffer.getAnsiContents()
        assertThat(bufferContents[0])
            .isEqualTo("${ansi(1,1)}Take my love,${ansi(0)}")
        assertThat(bufferContents[1])
            .isEqualTo("${ansi(1,1)}Take my land...${ansi(0)}")
    }

    @Test fun continueTrailingStyleAcrossLines() {
        val lineOne = "${ansi(1,2)}Take my love,${ansi(1,4)}\r\n"
        val lineTwo = "Take my land...\r\n"

        assertThat(OutputLine(lineOne.trimEnd()).getFinalStyle().toString())
            .isEqualTo(ansi(1,4).toString())

        val core = JudoCore(renderer, settings)
        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
        core.onIncomingBuffer("\r\n".toCharArray(), 2) // empty line
        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)

        val buffer = core.renderer.currentTabpage!!.currentWindow.currentBuffer
        if (buffer[0].startsWith("ERROR")) {
            throw AssertionError(buffer.getAnsiContents().joinToString("\n"))
        }

        assertThat(buffer.getAnsiContents()[2])
            .isEqualTo("${ansi(1,4)}Take my land...${ansi(0)}")
    }

    @Test fun continueTrailingStyleAcrossSplitLines() {
        renderer.windowWidth = 12
        renderer.windowHeight = 7
        renderer.updateSize()

        val lineOne = "\r\n${ansi(1,2)}\r\nTake my love\r\nTake"
        val lineTwo = " my land...\r\nTake me where..."

        assertThat(OutputLine(lineOne).getFinalStyle().toString())
            .isEqualTo(ansi(1,2).toString())

        val core = JudoCore(renderer, settings)
        // NOTE: input line takes 1 from the 7 above
        assertThat(renderer.currentTabpage!!.currentWindow.height).isEqualTo(6)

        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)

        val window = core.renderer.currentTabpage!!.currentWindow
        val buffer = window.currentBuffer
        if (buffer[0].startsWith("ERROR")) {
            throw AssertionError(buffer.getAnsiContents().joinToString("\n"))
        }

        val bufferContents = buffer.getAnsiContents()
        assertThat(bufferContents[2])
            .isEqualTo("${ansi(1,2)}Take my love${ansi(0)}")
        assertThat(bufferContents[3])
            .isEqualTo("${ansi(1,2)}Take my land${ansi(0)}")
        assertThat(bufferContents[4])
            .isEqualTo("${ansi(1,2)}...${ansi(0)}")
        assertThat(bufferContents[5])
            .isEqualTo("${ansi(1,2)}Take me where...${ansi(0)}")

        // NOTE: status line takes 1 from the 6 above
        val outputWindow = (window as PrimaryJudoWindow).outputWindow
        assertThat(outputWindow.height).isEqualTo(5)

        val windowContents = outputWindow.getDisplayStrings()
        assertThat(windowContents).hasSize(outputWindow.height)
        assertThat(windowContents[0])
            .isEqualTo("${ansi(1,2)}Take my love${ansi(0)}")
        assertThat(windowContents[1])
            .isEqualTo("${ansi(1,2)}Take my land${ansi(0)}")
        assertThat(windowContents[2])
            .isEqualTo("${ansi(1,2)}...${ansi(0)}")
        assertThat(windowContents[3])
            .isEqualTo("${ansi(1,2)}Take me wher${ansi(0)}")
    }

    @Test fun continueEmptyStyleAcrossLines() {
        val core = JudoCore(renderer, settings)

        val lineOne = "${ansi(1,1)}\r\n"
        val lineTwo = "Take my love...\r\n"

        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)

        val window = core.renderer.currentTabpage!!.currentWindow
        val buffer = window.currentBuffer
        if (buffer[0].startsWith("ERROR")) {
            throw AssertionError(buffer.getAnsiContents().joinToString("\n"))
        }

        assertThat(buffer.getAnsiContents()[1])
            .isEqualTo("${ansi(1,1)}Take my love...${ansi(0)}")
    }


    @Test fun promptRender() {
        window.appendLine("Take my love", isPartialLine = false)
        window.appendLine("Take my land", isPartialLine = false)
        window.updateStatusLine("[prompt]    <status>")
        window.promptWindow.resize(window.width, 2)
        window.promptBuffer.set(listOf("[prompt1]", "[prompt2]"))
        window.resize(window.width, window.height)
        renderer.updateInputLine("Test", 4)

        val (lines, cursorRow, cursorCol) = renderer.getDisplayLines()
        assertThat(lines.map { it.toString() })
            .containsExactly(
                "",
                "Take my love",
                "Take my land",
                "[prompt1]",
                "[prompt]    <status>",
                "Test"
            )

        assertThat(cursorRow).isEqualTo(5)
        assertThat(cursorCol).isEqualTo(4)
    }

    @Test fun cursorInStatusLine() {
        window.appendLine("Take my love", isPartialLine = false)
        window.appendLine("Take my land", isPartialLine = false)
        window.promptWindow.resize(window.width, 2)
        window.promptBuffer.set(listOf("[prompt1]", "[prompt2]"))
        window.resize(window.width, window.height)
        renderer.updateInputLine("Test", 4)
        window.updateStatusLine(":cmdMode", 8)

        val (lines, cursorRow, cursorCol) = renderer.getDisplayLines()
        assertThat(lines.map { it.toString() })
            .containsExactly(
                "",
                "Take my love",
                "Take my land",
                "[prompt1]",
                ":cmdMode",
                "Test"
            )

        assertThat(cursorRow).isEqualTo(4)
        assertThat(cursorCol).isEqualTo(8)
    }
}

fun IJudoBuffer.getAnsiContents(): List<String> =
    (0..this.lastIndex).map { (this[it] as OutputLine).toAttributedString().toAnsi() }

private fun JLineRenderer.typeAndFit(text: String, expected: Pair<String, Int>) {
    updateInputLine(text, text.length)

    val (line, cursor) = fitInputLineToWindow()
    assertThat(line.toString() to cursor).isEqualTo(expected)
}

private fun JLineRenderer.typeMultiAndFit(text: String, expected: Pair<List<String>, Pair<Int, Int>>) {
    updateInputLine(text, text.length)

    val (lines, cursor) = fitInputLinesToWindow()
    assertThat(lines.map { it.toString() } to cursor)
        .isEqualTo(expected)
}

fun IJudoWindow.getDisplayStrings(): List<String> =
    with(mutableListOf<CharSequence>()) {
        getDisplayLines(this)
        map { (it as AttributedString).toAnsi() }
    }
