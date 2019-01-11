package net.dhleong.judo.jline

import assertk.assert
import net.dhleong.judo.StateMap
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoTabpage2
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer2
import net.dhleong.judo.render.PrimaryJudoWindow2
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * @author dhleong
 */
class JLineTabpageTest {

    lateinit var display: JLineDisplay
    lateinit var settings: StateMap

    lateinit var renderer: JLineRenderer2
    lateinit var primary: PrimaryJudoWindow2
    lateinit var tabpage: IJudoTabpage2

    @Before fun setUp() {
        settings = StateMap()

        val width = 12
        val height = 7
        display = JLineDisplay(0, 0)
        renderer = JLineRenderer2(
            IdManager(),
            settings,
            enableMouse = false,
            renderSurface = display
        ).apply {
            forceResize(width, height)
            setLoading(false)
        }
        tabpage = renderer.currentTabpage
        primary = tabpage.currentWindow as PrimaryJudoWindow2
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

        val buffer = JudoBuffer2(IdManager())
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

    @Ignore
    @Test fun `Close window from a single split`() {
        primary.appendLine("Take my land")
        primary.updateStatusLine("[status]")
        assert(primary).hasHeight(6)

        val buffer = JudoBuffer2(IdManager())
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

        val buffer = JudoBuffer2(IdManager())
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

    @Ignore
    @Test fun `Resize a split`() {
        tabpage.resize(tabpage.width, 8)

        val buffer = JudoBuffer2(IdManager())
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love")

        assert(window).hasHeight(2)
        assert(primary).hasHeight(5) // NOTE: separator!

        window.resize(window.width, 4)
//        tabpage.resize()

        assert(window).hasHeight(4)
        assert(primary).hasHeight(3)
    }

    @Test fun `Re-split`() {
        primary.appendLine("Take my land")
        primary.updateStatusLine("[status]")

        val buffer = JudoBuffer2(IdManager())
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

private fun PrimaryJudoWindow2.updateStatusLine(line: String) {
    updateStatusLine(FlavorableStringBuilder.withDefaultFlavor(line))
}
