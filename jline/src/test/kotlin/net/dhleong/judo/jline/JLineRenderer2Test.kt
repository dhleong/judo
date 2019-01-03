package net.dhleong.judo.jline

import assertk.assert
import net.dhleong.judo.StateMap
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoWindow2
import net.dhleong.judo.render.IdManager
import org.junit.Before
import org.junit.Ignore
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
            updateSize()
            setLoading(false)
        }
        window = renderer.currentTabpage.currentWindow
    }

    @Ignore
    @Test fun `Basic render`() {
        window.appendLine("Take my love")
        window.appendLine("Take my land")
        window.updateStatusLine("<status>")
        renderer.updateInputLine("Test", 4)

        renderer.render(display)
        assert(display).linesEqual("""
            |____________________
            |____________________
            |Take my love________
            |Take my land________
            |<status>____________
            |Test________________
        """.trimMargin())
    }
}

private fun IJudoWindow2.appendLine(line: String) {
    currentBuffer.appendLine(FlavorableStringBuilder.withDefaultFlavor(line))
}
