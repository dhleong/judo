package net.dhleong.judo.render

import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.util.ansi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class JudoWindowTest {

    lateinit var window: JudoWindow
    lateinit var buffer: JudoBuffer
    val settings = StateMap(
        WORD_WRAP to false
    )

    @Before fun setUp() {
        val ids = IdManager()
        buffer = JudoBuffer(ids)
        window = JudoWindow(ids, settings, 42, 20, buffer, isFocusable = false)
    }

    @Test fun scrollBack() {
        window.width = 6
        window.height = 2

        window.appendLine("Take my love", isPartialLine = false)
        window.appendLine("Take my land", isPartialLine = false)
        window.appendLine("Take me where I cannot stand", isPartialLine = false)

        assertThat(window.getDisplayStrings())
            .containsExactly("nnot s", "tand")

        window.scrollPages(1)
        assertThat(window.getDisplayStrings())
            .containsExactly("e wher", "e I ca")

        window.scrollPages(1)
        assertThat(window.getDisplayStrings())
            .containsExactly("y land", "Take m")

        window.scrollPages(-1)
        assertThat(window.getDisplayStrings())
            .containsExactly("e wher", "e I ca")

        window.scrollPages(-1)
        assertThat(window.getDisplayStrings())
            .containsExactly("nnot s", "tand")
    }

    @Test fun scrollBackWrapping() {
        window.width = 42
        window.height = 2

        window.appendLine("Take My love", isPartialLine = false)
        window.appendLine("Take my land", isPartialLine = false)
        window.appendLine("Take me where I cannot stand", isPartialLine = false)
        assertThat(window.getDisplayStrings())
            .containsExactly(
                "Take my land",
                "Take me where I cannot stand")

        // now resize the window and force wrapping
        window.width = 6
        assertThat(window.getDisplayStrings())
            .containsExactly("nnot s", "tand")

        window.scrollPages(1) // bot=0; off=2
        assertThat(window.getDisplayStrings())
            .containsExactly("e wher", "e I ca")

        window.scrollPages(1) // bot=0; off=4
        assertThat(window.getDisplayStrings())
            .containsExactly("y land", "Take m")

        window.scrollPages(1) // bot=1; off=1
        assertThat(window.getDisplayStrings())
            .containsExactly("y love", "Take m")

        window.scrollPages(1)
        assertThat(window.getDisplayStrings())
            .containsExactly("", "Take M")

        // repeat; we're at the top
        window.scrollPages(1)
        assertThat(window.getDisplayStrings())
            .containsExactly("", "Take M")

        // go back down
        window.scrollPages(-1)
        assertThat(window.getDisplayStrings())
            .containsExactly("y love", "Take m")

        window.scrollPages(-2) // skip...
        assertThat(window.getDisplayStrings())
            .containsExactly("e wher", "e I ca")

        window.scrollPages(-1)
        assertThat(window.getDisplayStrings())
            .containsExactly("nnot s", "tand")
    }

    @Test fun maintainScrollback() {
        window.width = 42
        window.height = 2

        window.appendLine("Take My love", isPartialLine = false)
        window.appendLine("Take my land", isPartialLine = false)
        window.appendLine("Take me where I cannot stand", isPartialLine = false)
        assertThat(window.getDisplayStrings())
            .containsExactly(
                "Take my land",
                "Take me where I cannot stand")

        // now resize the window and force wrapping
        window.width = 6
        window.scrollPages(1)
        assertThat(window.getDisplayStrings())
            .containsExactly("e wher", "e I ca")

        window.appendLine("PAR", isPartialLine = true)
        window.appendLine("TS", isPartialLine = false)
        window.appendLine("LINES", isPartialLine = false)

        // since we're scrolled, we should stay
        // where we are
        assertThat(window.getDisplayStrings())
            .containsExactly("e wher", "e I ca")
    }

    @Test fun search() {
        window.width = 12
        window.height = 2

        window.appendLine("Take My love", isPartialLine = false)
        window.appendLine("Take my land", isPartialLine = false)
        window.appendLine("Take me where I cannot stand", isPartialLine = false)
        assertThat(window.getDisplayStrings())
            .containsExactly(
                "e I cannot s",
                "tand")

        window.searchForKeyword("m", direction = 1)
        assertThat(window.getDisplayStrings())
            .containsExactly(
                "Take my land",
                "Take ${ansi(inverse = true)}m${ansi(0)}e wher"
            )

        window.searchForKeyword("m", direction = 1)
        assertThat(window.getDisplayStrings())
            .containsExactly(
                "Take ${ansi(inverse = true)}m${ansi(0)}y land",
                "Take me wher"
            )

        // step back
        window.searchForKeyword("m", direction = -1)
        assertThat(window.getDisplayStrings())
            .containsExactly(
                "Take my land",
                "Take ${ansi(inverse = true)}m${ansi(0)}e wher"
            )

        // go to next page
        window.searchForKeyword("m", direction = 1)
        window.searchForKeyword("m", direction = 1)
        assertThat(window.getDisplayStrings())
            .containsExactly(
                "",
                "Take ${ansi(inverse = true)}M${ansi(0)}y love"
            )
    }

//    @Test fun continueStyleAcrossLines() {
//        val core = JudoCore(renderer, settings)
//
//        val lineOne = "${ansi(1,1)}Take my love,\r\n"
//        val lineTwo = "Take my land...\r\n"
//
//        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
//        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)
//
//        if (renderer.getDisplayStrings()[0].startsWith("ERROR")) {
//            throw AssertionError(renderer.getDisplayStrings().joinToString("\n"))
//        }
//
//        assertThat(renderer.getDisplayStrings()[0])
//            .isEqualTo("${ansi(1,1)}Take my love,${ansi(0)}")
//        assertThat(renderer.getDisplayStrings()[1])
//            .isEqualTo("${ansi(1,1)}Take my land...${ansi(0)}")
//    }
//
//    @Test fun continueTrailingStyleAcrossLines() {
//        val lineOne = "${ansi(1,2)}Take my love,${ansi(1,4)}\r\n"
//        val lineTwo = "Take my land...\r\n"
//
//        assertThat(OutputLine(lineOne.trimEnd()).getFinalStyle().toString())
//            .isEqualTo(ansi(1,4).toString())
//
//        val core = JudoCore(renderer, settings)
//        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
//        core.onIncomingBuffer("\r\n".toCharArray(), 2) // empty line
//        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)
//
//        if (renderer.getDisplayStrings()[0].startsWith("ERROR")) {
//            throw AssertionError(renderer.getDisplayStrings().joinToString("\n"))
//        }
//
//        assertThat(renderer.getDisplayStrings()[2])
//            .isEqualTo("${ansi(1,4)}Take my land...${ansi(0)}")
//    }
//
//    @Test fun continueTrailingStyleAcrossSplitLines() {
//        renderer.windowWidth = 12
//        val lineOne = "\r\n${ansi(1,2)}\r\nTake my love\r\nTake"
//        val lineTwo = " my land...\r\nTake me where..."
//
//        assertThat(OutputLine(lineOne).getFinalStyle().toString())
//            .isEqualTo(ansi(1,2).toString())
//
//        val core = JudoCore(renderer, settings)
//        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
//        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)
//
//        if (renderer.getDisplayStrings()[0].startsWith("ERROR")) {
//            throw AssertionError(renderer.getDisplayStrings().joinToString("\n"))
//        }
//
//        assertThat(renderer.getDisplayStrings()[2])
//            .isEqualTo("${ansi(1,2)}Take my love${ansi(0)}")
//        assertThat(renderer.getDisplayStrings()[3])
//            .isEqualTo("${ansi(1,2)}Take my land${ansi(0)}")
//        assertThat(renderer.getDisplayStrings()[4])
//            .isEqualTo("${ansi(1,2)}...${ansi(0)}")
//        assertThat(renderer.getDisplayStrings()[5])
//            .isEqualTo("${ansi(1,2)}Take me wher${ansi(0)}")
//    }
//
//    @Test fun continueEmptyStyleAcrossLines() {
//        val core = JudoCore(renderer, settings)
//
//        val lineOne = "${ansi(1,1)}\r\n"
//        val lineTwo = "Take my love...\r\n"
//
//        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
//        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)
//
//        if (renderer.getDisplayStrings()[0].startsWith("ERROR")) {
//            throw AssertionError(renderer.getDisplayStrings().joinToString("\n"))
//        }
//
//        assertThat(renderer.getDisplayStrings()[1])
//            .isEqualTo("${ansi(1,1)}Take my love...${ansi(0)}")
//    }
//
//    private fun JLineRenderer.typeAndFit(text: String, expected: Pair<String, Int>) {
//        updateInputLine(text, text.length)
//
//        val (line, cursor) = fitInputLineToWindow()
//        assertThat(line.toString() to cursor).isEqualTo(expected)
//    }
}