package net.dhleong.judo.jline

import assertk.all
import assertk.assert
import net.dhleong.judo.StateMap
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.PrimaryJudoWindow
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
