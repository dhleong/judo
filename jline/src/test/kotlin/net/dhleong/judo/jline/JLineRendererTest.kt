package net.dhleong.judo.jline

import assertk.all
import assertk.assert
import assertk.assertions.isTrue
import net.dhleong.judo.MAX_INPUT_LINES
import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.bufferOf
import net.dhleong.judo.hasWidth
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.PrimaryJudoWindow
import net.dhleong.judo.render.toFlavorable
import org.junit.Before
import org.junit.Test

class JLineRendererTest {

    lateinit var display: JLineDisplay
    lateinit var settings: StateMap

    lateinit var renderer: JLineRenderer
    lateinit var window: PrimaryJudoWindow

    @Before fun setUp() {
        settings = StateMap()

        val width = 20
        val height = 6
        display = JLineDisplay(0, 0)
        renderer = JLineRenderer(
            IdManager(),
            settings,
            enableMouse = false,
            renderSurface = display
        ).apply {
            forceResize(width, height)
            setLoading(false)
        }
        window = renderer.currentTabpage.currentWindow as PrimaryJudoWindow
    }

    @Test fun `Basic render`() {
        window.appendLine("Take my love")
        window.appendLine("Take my land")
        window.updateStatusLine("<status>")
        renderer.updateInputLine("Test", 4)

        renderer.render(display)
        assert(display).all {
            linesEqual("""
                |____________________
                |____________________
                |Take my love________
                |Take my land________
                |<status>____________
                |Test________________
            """.trimMargin())

            hasCursor(
                row = 5,
                col = 4
            )
        }
    }

    @Test fun `Render wrapped input line`() {
        settings[WORD_WRAP] = true
        settings[MAX_INPUT_LINES] = 3

        window.appendLine("Take my love")
        window.appendLine("Take my land")
        window.updateStatusLine("<status>")
        renderer.updateInputLine("Take me where I cannot stand", 4)

        renderer.render(display)
        assert(display).all {
            linesEqual("""
                |____________________
                |Take my love________
                |Take my land________
                |<status>____________
                |Take me where I_____
                |cannot stand________
            """.trimMargin())

            hasCursor(
                row = 4,
                col = 4
            )
        }
    }

    @Test fun `Reactive render`() {
        // changes to primary or buffer should trigger a render

        window.updateStatusLine("<status>")
        assert(display).linesEqual("""
            |____________________
            |____________________
            |____________________
            |____________________
            |<status>____________
            |____________________
        """.trimMargin())

        window.appendLine("Take my love")
        assert(display).linesEqual("""
            |____________________
            |____________________
            |____________________
            |Take my love________
            |<status>____________
            |____________________
        """.trimMargin())
    }

    @Test fun `Reactive resize`() {
        window.appendLine("Take my love")
        window.appendLine("Take my land")
        window.updateStatusLine("<status>")
        renderer.updateInputLine("Test", 4)

        renderer.forceResize(12, 5)

        assert(display).all {
            linesEqual("""
                |____________
                |Take my love
                |Take my land
                |<status>____
                |Test________
            """.trimMargin())

            hasCursor(
                row = 4,
                col = 4
            )
        }
    }

    @Test fun `Clean re-render of status`() {
        window.updateStatusLine("<status>")
        assert(display).linesEqual("""
            |____________________
            |____________________
            |____________________
            |____________________
            |<status>____________
            |____________________
        """.trimMargin())

        window.updateStatusLine("<>")
        assert(display).linesEqual("""
            |____________________
            |____________________
            |____________________
            |____________________
            |<>__________________
            |____________________
        """.trimMargin())
    }

    @Test fun `Clean re-render of input`() {
        window.updateStatusLine("<status>")
        renderer.updateInputLine("<input>", 0)
        assert(display).linesEqual("""
            |____________________
            |____________________
            |____________________
            |____________________
            |<status>____________
            |<input>_____________
        """.trimMargin())

        renderer.updateInputLine("<>", 0)
        assert(display).linesEqual("""
            |____________________
            |____________________
            |____________________
            |____________________
            |<status>____________
            |<>__________________
        """.trimMargin())
    }

    @Test fun `Scroll window after buffer append`() {
        // changes to primary or buffer should trigger a render

        window.updateStatusLine("<status>")
        window.appendLine("Take my love")
        window.appendLine("Take my land")
        window.appendLine("Take me where I")
        window.appendLine("cannot stand")
        window.appendLine("I don't care")
        window.appendLine("I'm still free")
        assert(display).linesEqual("""
            |Take me where I_____
            |cannot stand________
            |I don't care________
            |I'm still free______
            |<status>____________
            |____________________
        """.trimMargin())
    }

    @Test fun `Basic prompt rendering`() {
        window.appendLine("Take my love")
        window.appendLine("Take my land")
        window.updateStatusLine("[prompt]    <status>")
        window.setPromptHeight(2)
        window.promptWindow.currentBuffer.set(listOf(
            "[prompt1]", "[prompt2]"
        ).map { FlavorableStringBuilder.withDefaultFlavor(it) })
        window.resize(window.width, window.height)
        renderer.updateInputLine("Test", 4)

        assert(display).all {
            linesEqual("""
                |____________________
                |Take my love________
                |Take my land________
                |[prompt1]___________
                |[prompt]____<status>
                |Test________________
            """.trimMargin())
            hasCursor(5, 4)
        }
    }

    @Test fun `Cursor in status line`() {
        window.appendLine("Take my love")
        window.appendLine("Take my land")
        window.setPromptHeight(2)
        window.promptWindow.currentBuffer.set(listOf(
            "[prompt1]",
            "[prompt2]"
        ).map { FlavorableStringBuilder.withDefaultFlavor(it) })
        window.resize(window.width, window.height)
        renderer.updateInputLine("Test", 4)
        window.updateStatusLine(":cmdMode", 8)

        assert(display).all {
            linesEqual("""
                |____________________
                |Take my love________
                |Take my land________
                |[prompt1]___________
                |:cmdMode____________
                |Test________________
            """.trimMargin())
            hasCursor(4, 8)
        }
    }

    @Test fun `Cursor in wrapped status line`() {
        renderer.updateInputLine("<input>", 7)
        window.updateStatusLine("Take me where I cannot stand", 28)

        assert(display).all {
            linesEqual("""
                |____________________
                |____________________
                |____________________
                |____________________
                |…re I cannot stand__
                |<input>_____________
            """.trimMargin())
            hasCursor(4, 18)
        }
    }

    @Test fun `Cursor in status line with wrapped input`() {
        settings[WORD_WRAP] = true
        settings[MAX_INPUT_LINES] = 2
        window.appendLine("Take my love")
        window.appendLine("Take my land")
        window.resize(window.width, window.height)
        renderer.updateInputLine("Take me where I cannot stand", 4)
        window.updateStatusLine(":cmdMode", 8)

        assert(display).all {
            linesEqual("""
                |____________________
                |Take my love________
                |Take my land________
                |:cmdMode____________
                |Take me where I_____
                |cannot stand________
            """.trimMargin())
            hasCursor(3, 8)
        }
    }

    @Test fun `Render ellipsized input`() {
        window.updateStatusLine("<status>")
        renderer.updateInputLine("Take me where I cannot stand", 28)

        assert(display).all {
            linesEqual("""
                |____________________
                |____________________
                |____________________
                |____________________
                |<status>____________
                |…re I cannot stand__
            """.trimMargin())
            hasCursor(5, 18)
        }
    }

    @Test fun `Render echo() single-line output`() {
        renderer.echo(FlavorableStringBuilder.withDefaultFlavor("Take my love"))

        assert(display).all {
            linesEqual("""
                |____________________
                |____________________
                |____________________
                |____________________
                |Take my love________
                |____________________
            """.trimMargin())
        }
    }

    @Test fun `Clear echo() output on scroll`() {
        window.updateStatusLine("<status>")
        renderer.echo(FlavorableStringBuilder.withDefaultFlavor("Take my love"))
        window.scrollPages(1)

        // reset to previously-set status
        assert(display).all {
            linesEqual("""
                |____________________
                |____________________
                |____________________
                |____________________
                |<status>____________
                |____________________
            """.trimMargin())
        }
    }

    @Test fun `Multi-line echo()`() {
        window.appendLine("Mal Reynolds")
        window.updateStatusLine("<status>")
        renderer.echo(FlavorableStringBuilder.withDefaultFlavor("Take my love\nTake my land"))

        assert(display).all {
            linesEqual("""
                |Mal Reynolds________
                |<status>____________
                |Take my love________
                |Take my land________
                |Press ENTER or type_
                |command to continue_
            """.trimMargin())
            hasCursor(
                5, 19
            )
        }
    }

    @Test fun `Forced Multi-line echo()`() {
        window.appendLine("Mal Reynolds")
        window.updateStatusLine("<status>")
        renderer.echo(FlavorableStringBuilder.withDefaultFlavor("Take my\nlove"))

        assert(display).all {
            linesEqual("""
                |Mal Reynolds________
                |<status>____________
                |Take my ____________
                |love________________
                |Press ENTER or type_
                |command to continue_
            """.trimMargin())
            hasCursor(
                5, 19
            )
        }
    }

    @Test fun `Render vertical split`() {
        window.appendLine("Mal Reynolds")
        window.updateStatusLine("<status>")

        val splitBuffer = bufferOf("""
            Take my love
        """.trimIndent())
        val right = renderer.currentTabpage.vsplit(8, splitBuffer)
        right.updateStatusLine("[right]")

        assert(settings[WORD_WRAP]).isTrue()
        assert(window).hasWidth(11)

        assert(display).all {
            linesEqual("""
                |___________ ________
                |___________ ________
                |Mal________ Take my_
                |Reynolds___ love____
                |----------- [right]_
                |___________ ________
            """.trimMargin())
        }
    }

    @Test fun `Cursor placement with vertical split status`() {
        val splitBuffer = bufferOf("""
            Take my love
        """.trimIndent())
        val right = renderer.currentTabpage.vsplit(8, splitBuffer)
        right.updateStatusLine(":right", 6)

        assert(display).all {
            linesEqual("""
                |___________ ________
                |___________ ________
                |___________ Take my_
                |___________ love____
                |----------- :right__
                |___________ ________
            """.trimMargin())

            hasCursor(4, 18)
        }
    }

    @Test fun `Cursor placement with multi-split status`() {
        val rightBuffer = bufferOf("""
            Take my land
        """.trimIndent())
        renderer.currentTabpage.vsplit(7, rightBuffer)

        renderer.currentTabpage.currentWindow = window
        val topBuffer = bufferOf("""
            Take my love
        """.trimIndent())
        renderer.currentTabpage.hsplit(1, topBuffer)

        renderer.currentTabpage.currentWindow = window
        window.updateStatusLine(":prim", 5)

        assert(display).all {
            linesEqual("""
                |Take my love _______
                |------------ _______
                |____________ Take my
                |____________ land___
                |:prim_______ -------
                |____________ _______
            """.trimMargin())

            hasCursor(4, 5)
        }
    }

}

private fun JLineRenderer.updateInputLine(line: String, cursor: Int) {
    updateInputLine(line.toFlavorable(), cursor)
}

fun JLineRenderer.forceResize(width: Int, height: Int) {
    windowWidth = width
    windowHeight = height
    windowSize.rows = height
    windowSize.columns = width
    updateSize()
}

private fun IJudoWindow.updateStatusLine(status: String, cursor: Int = -1) {
    updateStatusLine(FlavorableStringBuilder.withDefaultFlavor(status), cursor)
}
