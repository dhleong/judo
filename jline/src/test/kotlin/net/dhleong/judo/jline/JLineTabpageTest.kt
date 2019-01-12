package net.dhleong.judo.jline

import assertk.assert
import net.dhleong.judo.StateMap
import net.dhleong.judo.hasHeight
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.render.PrimaryJudoWindow
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class JLineTabpageTest {

    lateinit var display: JLineDisplay
    lateinit var settings: StateMap

    lateinit var renderer: JLineRenderer
    lateinit var primary: PrimaryJudoWindow
    lateinit var tabpage: IJudoTabpage

    @Before fun setUp() {
        settings = StateMap()

        val width = 12
        val height = 7
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
        tabpage = renderer.currentTabpage
        primary = tabpage.currentWindow as PrimaryJudoWindow
    }

    @Test fun `Render hsplit window`() {
        primary.appendLine("Take my land")
        primary.updateStatusLine("[status]")
        assert(display).linesEqual("""
            |____________
            |____________
            |____________
            |____________
            |Take my land
            |[status]____
            |____________
        """.trimMargin())

        val buffer = JudoBuffer(IdManager())
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love")

        assert(window).hasHeight(2)
        assert(primary).hasHeight(3)

        assert(display).linesEqual("""
            |_          _
            |Take my love
            |------------
            |_          _
            |Take my land
            |[status]____
            |_          _
        """.trimMargin())
    }

    @Test fun `Close window from a single split`() {
        primary.appendLine("Take my land")
        primary.updateStatusLine("[status]")
        assert(primary).hasHeight(6)

        val buffer = JudoBuffer(IdManager())
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love")

        // close
        tabpage.close(window)

        assert(primary).hasHeight(6)
        assert(display).linesEqual("""
            |____________
            |____________
            |____________
            |____________
            |Take my land
            |[status]____
            |____________
        """.trimMargin())
    }

    @Test fun `Tabpage unsplit`() {
        primary.appendLine("Take my land")
        primary.updateStatusLine("[status]")

        val buffer = JudoBuffer(IdManager())
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love")

        assert(window).hasHeight(2)
        assert(primary).hasHeight(3)

        assert(display).linesEqual("""
            |____________
            |Take my love
            |------------
            |____________
            |Take my land
            |[status]____
            |____________
        """.trimMargin())

        tabpage.unsplit()
        assert(display).linesEqual("""
            |____________
            |____________
            |____________
            |____________
            |Take my land
            |[status]____
            |____________
        """.trimMargin())

        // do it again
        tabpage.unsplit()
        assert(display).linesEqual("""
            |____________
            |____________
            |____________
            |____________
            |Take my land
            |[status]____
            |____________
        """.trimMargin())
    }

    @Test fun `Resize a split`() {
        renderer.forceResize(tabpage.width, 9)
        assert(tabpage).hasHeight(8)
        assert(primary).hasHeight(8)
        tabpage.resize(tabpage.width, 8) // should not be a problem

        val buffer = JudoBuffer(IdManager())
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love")

        assert(window).hasHeight(2)
        assert(primary).hasHeight(5) // NOTE: separator!

        window.resize(window.width, 4)

        assert(window).hasHeight(4)
        assert(primary).hasHeight(3)
    }

    @Test fun `Re-split`() {
        primary.appendLine("Take my land")
        primary.updateStatusLine("[status]")

        val buffer = JudoBuffer(IdManager())
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love")

        assert(window).hasHeight(2)
        assert(primary).hasHeight(3)

        // make sure we're good and unsplit
        tabpage.unsplit()
        tabpage.unsplit() // this should be idempotent

        // now, re-split without erroring
        tabpage.hsplit(2, buffer)
        assert(display).linesEqual("""
            |_          _
            |Take my love
            |------------
            |_          _
            |Take my land
            |[status]____
            |_          _
        """.trimMargin())
    }
}

private fun PrimaryJudoWindow.updateStatusLine(line: String) {
    updateStatusLine(FlavorableStringBuilder.withDefaultFlavor(line))
}
