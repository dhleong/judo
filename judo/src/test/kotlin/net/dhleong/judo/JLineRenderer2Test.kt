package net.dhleong.judo

import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.render.JudoTabpage
import net.dhleong.judo.render.PrimaryJudoWindow
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class JLineRenderer2Test {
    lateinit var renderer: JLineRenderer2
    lateinit var window: IJudoWindow
    lateinit var page: IJudoTabpage

    val settings = StateMap(
        WORD_WRAP to false
    )

    @Before fun setUp() {
        val ids = IdManager()
        val buffer = JudoBuffer(ids.newBuffer(), false)

        window = PrimaryJudoWindow(ids, settings, buffer, 20, 5)
        window.isFocused = true
        page = JudoTabpage(ids, settings, window)

        renderer = JLineRenderer2(settings, false)
        renderer.windowWidth = 20
        renderer.windowHeight = 6
        renderer.currentTabpage = page
        page.resize(renderer.windowWidth, renderer.windowHeight - 1)
    }

    @After fun tearDown() {
        renderer.close()
    }

    @Test fun basicRender() {
        window.appendLine("Take my love", isPartialLine = false)
        window.appendLine("Take my land", isPartialLine = false)
        window.updateStatusLine("<status>")
        renderer.updateInputLine("Test", 4)

        val windowLines = mutableListOf<CharSequence>()
        window.getDisplayLines(windowLines)
        assertThat(windowLines.map { it.toString() })
            .containsExactly(
                "",
                "",
                "Take my love",
                "Take my land",
                "<status>"
            )

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

        assertThat(cursorRow).isEqualTo(6)
        assertThat(cursorCol).isEqualTo(4)
    }
}