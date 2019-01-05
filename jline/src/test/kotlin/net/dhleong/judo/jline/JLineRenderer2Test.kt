package net.dhleong.judo.jline

import assertk.Assert
import assertk.assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.StateMap
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoWindow2
import net.dhleong.judo.render.IdManager
import org.junit.Before
import org.junit.Test

class JLineRenderer2Test {

    lateinit var display: JLineDisplay
    lateinit var settings: StateMap

    lateinit var renderer: JLineRenderer2
    lateinit var window: IJudoWindow2

    @Before fun setUp() {
        settings = StateMap()

        val width = 20
        val height = 6
        display = JLineDisplay(0, 0)
        renderer = JLineRenderer2(
            IdManager(),
            settings,
            enableMouse = false,
            renderSurface = display
        ).apply {
            windowWidth = width
            windowHeight = height
            windowSize.rows = height
            windowSize.columns = width
            updateSize()
            setLoading(false)
        }
        window = renderer.currentTabpage.currentWindow
    }

    @Test fun `Basic render`() {
        window.appendLine("Take my love")
        window.appendLine("Take my land")
        window.updateStatusLine("<status>")
        renderer.updateInputLine("Test", 4)

        // render with the internal method to get the cursor position
        val cursor = renderer.render(display)
        assert(display).linesEqual("""
            |____________________
            |____________________
            |Take my love________
            |Take my land________
            |<status>____________
            |Test________________
        """.trimMargin())
        assert(cursor).isCursor(
            row = 5,
            col = 4
        )
    }

    @Test fun `Reactive render`() {
        // changes to window or buffer should trigger a render

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

    private fun Assert<Int>.isCursor(row: Int, col: Int) {
        val actualCol = actual % (renderer.windowWidth + 1)
        val actualRow = (actual - actualCol) / (renderer.windowWidth + 1)
        if (row == actualRow && col == actualCol) return
        expected("cursor pos ${show(row to col)} but was ${show(actualRow to actualCol)}")
    }
}

private fun IJudoWindow2.appendLine(line: String) {
    currentBuffer.appendLine(FlavorableStringBuilder.withDefaultFlavor(line))
}
