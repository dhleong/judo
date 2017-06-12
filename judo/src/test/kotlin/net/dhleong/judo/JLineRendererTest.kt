package net.dhleong.judo

import net.dhleong.judo.render.OutputLine
import net.dhleong.judo.util.ansi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class JLineRendererTest {

    lateinit var renderer: JLineRenderer

    @Before fun setUp() {
        renderer = JLineRenderer()
        renderer.windowWidth = 42
        renderer.windowHeight = 22
        renderer.outputWindowHeight = 20
    }

    @After fun tearDown() {
        renderer.close()
    }

    @Test fun appendOutput_empty() {

        // basically make sure it doesn't crash
        renderer.appendOutput(OutputLine(""), isPartialLine = true)
        renderer.appendOutput(OutputLine(""), isPartialLine = false)

        assertThat(renderer.getDisplayStrings())
            .containsExactly("")
    }

    @Test fun appendOutput_resumePartial() {
        // append without a line end and continue that line
        // in a separate append. It's TCP so stuff happens

        renderer.appendOutput("", isPartialLine = false)
        renderer.appendOutput("Take my love,", isPartialLine = false)
        renderer.appendOutput("Take my", isPartialLine = true)
        renderer.appendOutput(" land,", isPartialLine = false)
        renderer.appendOutput("Take me where...", isPartialLine = false)
        renderer.appendOutput("I don't care, I'm still free", isPartialLine = false)

        assertThat(renderer.getDisplayStrings())
            .containsExactly(
                "",
                "Take my love,",
                "Take my land,",
                "Take me where...",
                "I don't care, I'm still free"
            )
        assertThat(renderer.getScrollback()).isEqualTo(0)
    }

    @Test fun appendOutput_resumePartial_continueAnsi() {
        // append without a line end and continue that line
        // in a separate append. It's TCP so stuff happens

        renderer.appendOutput("", isPartialLine = false)
        renderer.appendOutput("${ansi(1,6)}Take my ", isPartialLine = true)
        renderer.appendOutput("love", isPartialLine = false)

        assertThat(renderer.getDisplayStrings())
            .containsExactly(
                "",
                "${ansi(1,6)}Take my love${ansi(0)}"
            )
        assertThat(renderer.getScrollback()).isEqualTo(0)
    }

    @Test fun appendOutput_resumePartial_continueAnsi2() {

        // continuation of the partial line has its own ansi;
        // use previous line's ansi to start, but don't stomp
        // on the new ansi
        val first = OutputLine("${ansi(1,6)}Take my ")
        val second = OutputLine("lo${ansi(1,7)}ve")
        renderer.appendOutput(first, isPartialLine = true)
        renderer.appendOutput(second, isPartialLine = false)

        assertThat(renderer.getDisplayStrings()).hasSize(1)
        assertThat(renderer.getDisplayStrings()[0])
            .isEqualTo(
                "${ansi(1,6)}Take my lo${ansi(fg=7)}ve${ansi(0)}"
            )
        assertThat(renderer.getScrollback()).isEqualTo(0)
    }

    @Test fun appendOutput_resumePartial_trailingAnsi() {
        // append without a line end and continue that line
        // in a separate append. It's TCP so stuff happens

        val trailingAnsiLine = OutputLine("${ansi(1,6)}Take my ${ansi(1,2)}")
        renderer.appendOutput("", isPartialLine = false)
        renderer.appendOutput(trailingAnsiLine, isPartialLine = true)
        renderer.appendOutput("love", isPartialLine = false)

        assertThat(renderer.getDisplayStrings())
            .containsExactly(
                "",
                "${ansi(1,6)}Take my ${ansi(fg=2)}love${ansi(0)}"
            )
        assertThat(renderer.getScrollback()).isEqualTo(0)
    }

    @Test fun appendOutput_resumePartial_splitAnsi() {
        // append without a line end and continue that line
        // in a separate append. It's TCP so stuff happens

        val ansi = ansi(1,2)
        val firstHalf = ansi.slice(0..3)
        val secondHalf = ansi.slice(4..ansi.lastIndex)
        assertThat("$firstHalf$secondHalf").isEqualTo(ansi.toString())

        renderer.appendOutput(
            OutputLine("${ansi(1,6)}Take my $firstHalf"),
            isPartialLine = true)
        renderer.appendOutput(
            OutputLine("${secondHalf}love"),
            isPartialLine = false)

        assertThat(renderer.getDisplayStrings())
            .containsExactly(
                "${ansi(1,6)}Take my ${ansi(fg=2)}love${ansi(0)}"
            )
        assertThat(renderer.getScrollback()).isEqualTo(0)
    }

    @Test fun appendOutput_resumePartial_splitAnsi_integration() {
        // we've tested the core handling above, so let's make sure
        // JudoCore integrates into it correctly
        val core = JudoCore(renderer)

        val ansi = ansi(fg=2)
        val firstHalf = ansi.slice(0..ansi.lastIndex-1)
        val secondHalf = ansi.slice(ansi.lastIndex..ansi.lastIndex)
        assertThat("$firstHalf$secondHalf").isEqualTo(ansi.toString())
        assertThat("$secondHalf").isEqualTo("m")

        val lineOne = "${ansi(1,1)}Take my $firstHalf"
        val lineTwo = "${secondHalf}l${ansi(1,6)}ove"

        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)

        if (renderer.getDisplayStrings()[0].startsWith("ERROR")) {
            throw AssertionError(renderer.getDisplayStrings().joinToString("\n"))
        }

        assertThat(renderer.getDisplayStrings()[0])
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

    @Test fun catchSplitPrompts() {

        val judo = JudoCore(renderer)
        judo.prompts.define("HP: $1", "(hp: $1)")

        val prompt = "${ansi(1,3)}HP: ${ansi(1,6)}42\r\n"
        val first = prompt.substring(0..8)
        val second = prompt.substring(9..prompt.lastIndex)
        assertThat("$first$second").isEqualTo(prompt)

        judo.onIncomingBuffer(first.toCharArray(), first.length)
        judo.onIncomingBuffer(second.toCharArray(), second.length)

        assertThat(renderer.getDisplayStrings())
            .containsExactly("")
    }

    @Test fun catchSplitPrompts_splitAnsi() {

        val judo = JudoCore(renderer)
        judo.prompts.define("HP: $1", "(hp: $1)")

        val prompt = "${ansi(1,3)}HP: ${ansi(1,6)}42\r\n"
        val first = prompt.substring(0..5)
        val second = prompt.substring(6..prompt.lastIndex)
        assertThat("$first$second").isEqualTo(prompt)

        judo.onIncomingBuffer(first.toCharArray(), first.length)
        judo.onIncomingBuffer(second.toCharArray(), second.length)

        assertThat(renderer.getDisplayStrings())
            .containsExactly("")
    }

    @Test fun scrollBack() {
        renderer.windowWidth = 6
        renderer.windowHeight = 4
        renderer.outputWindowHeight = 2

        renderer.appendOutput("Take my love")
        renderer.appendOutput("Take my land")
        renderer.appendOutput("Take me where I cannot stand")

        assertThat(renderer.getDisplayStrings())
            .containsExactly("nnot s", "tand")

        renderer.scrollPages(1)
        assertThat(renderer.getDisplayStrings())
            .containsExactly("e wher", "e I ca")

        renderer.scrollPages(1)
        assertThat(renderer.getDisplayStrings())
            .containsExactly("y land", "Take m")

        renderer.scrollPages(-1)
        assertThat(renderer.getDisplayStrings())
            .containsExactly("e wher", "e I ca")

        renderer.scrollPages(-1)
        assertThat(renderer.getDisplayStrings())
            .containsExactly("nnot s", "tand")
    }

    @Test fun scrollBackWrapping() {
        renderer.windowWidth = 42
        renderer.windowHeight = 4
        renderer.outputWindowHeight = 2

        renderer.appendOutput("Take My love")
        renderer.appendOutput("Take my land")
        renderer.appendOutput("Take me where I cannot stand")
        assertThat(renderer.getDisplayStrings())
            .containsExactly(
                "Take my land",
                "Take me where I cannot stand")

        // now resize the window and force wrapping
        renderer.windowWidth = 6
        assertThat(renderer.getDisplayStrings())
            .containsExactly("nnot s", "tand")

        renderer.scrollPages(1) // bot=0; off=2
        assertThat(renderer.getDisplayStrings())
            .containsExactly("e wher", "e I ca")

        renderer.scrollPages(1) // bot=0; off=4
        assertThat(renderer.getDisplayStrings())
            .containsExactly("y land", "Take m")

        renderer.scrollPages(1) // bot=1; off=1
        assertThat(renderer.getDisplayStrings())
            .containsExactly("y love", "Take m")

        renderer.scrollPages(1)
        assertThat(renderer.getDisplayStrings())
            .containsExactly("Take M")

        // repeat; we're at the top
        renderer.scrollPages(1)
        assertThat(renderer.getDisplayStrings())
            .containsExactly("Take M")

        // go back down
        renderer.scrollPages(-1)
        assertThat(renderer.getDisplayStrings())
            .containsExactly("y love", "Take m")

        renderer.scrollPages(-2) // skip...
        assertThat(renderer.getDisplayStrings())
            .containsExactly("e wher", "e I ca")

        renderer.scrollPages(-1)
        assertThat(renderer.getDisplayStrings())
            .containsExactly("nnot s", "tand")
    }

    @Test fun maintainScrollback() {
        renderer.windowWidth = 42
        renderer.windowHeight = 4
        renderer.outputWindowHeight = 2

        renderer.appendOutput("Take My love")
        renderer.appendOutput("Take my land")
        renderer.appendOutput("Take me where I cannot stand")
        assertThat(renderer.getDisplayStrings())
            .containsExactly(
                "Take my land",
                "Take me where I cannot stand")

        // now resize the window and force wrapping
        renderer.windowWidth = 6
        renderer.scrollPages(1)
        assertThat(renderer.getDisplayStrings())
            .containsExactly("e wher", "e I ca")

        renderer.appendOutput("PAR", isPartialLine = true)
        renderer.appendOutput("TS", isPartialLine = false)
        renderer.appendOutput("LINES")

        // since we're scrolled, we should stay
        // where we are
        assertThat(renderer.getDisplayStrings())
            .containsExactly("e wher", "e I ca")
    }

    @Test fun continueStyleAcrossLines() {
        val core = JudoCore(renderer)

        val lineOne = "${ansi(1,1)}Take my love,\r\n"
        val lineTwo = "Take my land...\r\n"

        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)

        if (renderer.getDisplayStrings()[0].startsWith("ERROR")) {
            throw AssertionError(renderer.getDisplayStrings().joinToString("\n"))
        }

        assertThat(renderer.getDisplayStrings()[0])
            .isEqualTo("${ansi(1,1)}Take my love,${ansi(0)}")
        assertThat(renderer.getDisplayStrings()[1])
            .isEqualTo("${ansi(1,1)}Take my land...${ansi(0)}")
    }

    @Test fun continueTrailingStyleAcrossLines() {
        val lineOne = "${ansi(1,2)}Take my love,${ansi(1,4)}\r\n"
        val lineTwo = "Take my land...\r\n"

        assertThat(OutputLine(lineOne.trimEnd()).getFinalStyle().toString())
            .isEqualTo(ansi(1,4).toString())

        val core = JudoCore(renderer)
        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
        core.onIncomingBuffer("\r\n".toCharArray(), 2) // empty line
        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)

        if (renderer.getDisplayStrings()[0].startsWith("ERROR")) {
            throw AssertionError(renderer.getDisplayStrings().joinToString("\n"))
        }

        assertThat(renderer.getDisplayStrings()[2])
            .isEqualTo("${ansi(1,4)}Take my land...${ansi(0)}")
    }

    @Test fun continueTrailingStyleAcrossSplitLines() {
        renderer.windowWidth = 12
        val lineOne = "\r\n${ansi(1,2)}\r\nTake my love\r\nTake"
        val lineTwo = " my land...\r\nTake me where..."

        assertThat(OutputLine(lineOne).getFinalStyle().toString())
            .isEqualTo(ansi(1,2).toString())

        val core = JudoCore(renderer)
        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)

        if (renderer.getDisplayStrings()[0].startsWith("ERROR")) {
            throw AssertionError(renderer.getDisplayStrings().joinToString("\n"))
        }

        assertThat(renderer.getDisplayStrings()[2])
            .isEqualTo("${ansi(1,2)}Take my love${ansi(0)}")
        assertThat(renderer.getDisplayStrings()[3])
            .isEqualTo("${ansi(1,2)}Take my land${ansi(0)}")
        assertThat(renderer.getDisplayStrings()[4])
            .isEqualTo("${ansi(1,2)}...${ansi(0)}")
        assertThat(renderer.getDisplayStrings()[5])
            .isEqualTo("${ansi(1,2)}Take me wher${ansi(0)}")
    }

    @Test fun continueEmptyStyleAcrossLines() {
        val core = JudoCore(renderer)

        val lineOne = "${ansi(1,1)}\r\n"
        val lineTwo = "Take my love...\r\n"

        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)

        if (renderer.getDisplayStrings()[0].startsWith("ERROR")) {
            throw AssertionError(renderer.getDisplayStrings().joinToString("\n"))
        }

        assertThat(renderer.getDisplayStrings()[1])
            .isEqualTo("${ansi(1,1)}Take my love...${ansi(0)}")
    }

    private fun JLineRenderer.typeAndFit(text: String, expected: Pair<String, Int>) {
        updateInputLine(text, text.length)

        val (line, cursor) = fitInputLineToWindow()
        assertThat(line.toString() to cursor).isEqualTo(expected)
    }
}

private fun JLineRenderer.getDisplayStrings(): List<String> =
    getDisplayLines().map { it.toAttributedString().toAnsi() }

