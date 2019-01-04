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

        display = JLineDisplay(20, 6)
        renderer = JLineRenderer2(IdManager(), settings, enableMouse = false).apply {
            windowWidth = display.width
            windowHeight = display.height
            windowSize.rows = display.height
            windowSize.columns = display.width
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
