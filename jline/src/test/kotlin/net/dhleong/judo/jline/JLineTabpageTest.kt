package net.dhleong.judo.jline

import assertk.all
import assertk.assertThat
import assertk.assertions.isSameAs
import net.dhleong.judo.EmptyStateMap
import net.dhleong.judo.StateMap
import net.dhleong.judo.bufferOf
import net.dhleong.judo.emptyBuffer
import net.dhleong.judo.hasHeight
import net.dhleong.judo.hasWidth
import net.dhleong.judo.isFocused
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.render.PrimaryJudoWindow
import net.dhleong.judo.render.toFlavorable
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
    lateinit var tabpage: JLineTabpage

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
        tabpage = renderer.currentTabpage as JLineTabpage
        primary = tabpage.currentWindow as PrimaryJudoWindow
    }

    @Test fun `Render hsplit window`() {
        primary.appendLine("Take my land")
        primary.updateStatusLine("[pstatus]")
        assertThat(display).linesEqual("""
            |____________
            |____________
            |____________
            |____________
            |Take my land
            |[pstatus]___
            |____________
        """.trimMargin())

        val buffer = JudoBuffer(IdManager(), EmptyStateMap)
        val window = tabpage.hsplit(2, buffer)
        window.updateStatusLine("<status>".toFlavorable())
        window.appendLine("Take my love")

        assertThat(window).hasHeight(3)
        assertThat(primary).hasHeight(3)

        assertThat(display).linesEqual("""
            |_          _
            |Take my love
            |<status>____
            |_          _
            |Take my land
            |------------
            |_          _
        """.trimMargin())

        tabpage.currentWindow = primary
        assertThat(display).linesEqual("""
            |_          _
            |Take my love
            |------------
            |_          _
            |Take my land
            |[pstatus]___
            |_          _
        """.trimMargin())
    }

    @Test fun `Render vsplit window`() {
        renderer.forceResize(25, 5)
        primary.appendLine("Take my love")
        primary.updateStatusLine("[lstatus]")
        assertThat(display).linesEqual("""
            |____________ ____________
            |____________ ____________
            |Take my love ____________
            |[lstatus]___ ____________
            |____________ ____________
        """.trimMargin())

        val buffer = bufferOf("""
            Take my land
        """.trimIndent())
        val window = tabpage.vsplit(12, buffer)
        window.updateStatusLine("<rstatus>".toFlavorable())

        assertThat(window).hasWidth(12)
        assertThat(primary).hasWidth(12)

        assertThat(display).linesEqual("""
            |____________ ____________
            |____________ ____________
            |Take my love Take my land
            |------------ <rstatus>___
            |____________ ____________
        """.trimMargin())

        tabpage.currentWindow = primary
        assertThat(display).linesEqual("""
            |____________ ____________
            |____________ ____________
            |Take my love Take my land
            |[lstatus]___ ------------
            |____________ ____________
        """.trimMargin())
    }

    @Test fun `Render multi-split tabpage`() {
        renderer.forceResize(25, 6)
        primary.appendLine("Take my love")
        primary.updateStatusLine("[lstatus]")

        val botRight = tabpage.vsplit(12, bufferOf("""
            Take my land
        """.trimIndent()))
        botRight.updateStatusLine("<rstatus>".toFlavorable())

        assertThat(botRight).hasWidth(12)
        assertThat(primary).hasWidth(12)

        val topRight = tabpage.hsplit(2, bufferOf("""
            Serenity
        """.trimIndent()))
        topRight.updateStatusLine("(trstatus)".toFlavorable())

        assertThat(topRight).all {
            hasHeight(3)
            hasWidth(12)
        }
        assertThat(botRight).all {
            hasHeight(2)
            hasWidth(12)
        }
        assertThat(primary).all {
            hasHeight(5)
            hasWidth(12)
        }

        assertThat(display).linesEqual("""
            |____________ ____________
            |____________ Serenity____
            |____________ (trstatus)__
            |Take my love Take my land
            |------------ ------------
            |____________ ____________
        """.trimMargin())
    }

    @Test fun `Move focus around multi-split tabpage`() {
        renderer.forceResize(25, 6)
        primary.appendLine("Take my love")
        primary.updateStatusLine("[lstatus]")

        val botRight = tabpage.vsplit(12, bufferOf("""
            Take my land
        """.trimIndent()))
        botRight.updateStatusLine("<rstatus>".toFlavorable())

        val topRight = tabpage.hsplit(2, bufferOf("""
            Serenity
        """.trimIndent()))
        topRight.updateStatusLine("(trstatus)".toFlavorable())

        tabpage.focusLeft()
        assertThat(display).linesEqual("""
            |____________ ____________
            |____________ Serenity____
            |____________ ------------
            |Take my love Take my land
            |[lstatus]___ ------------
            |____________ ____________
        """.trimMargin())

        tabpage.focusRight(2) // going too far should be safe
        tabpage.focusDown() // should be nop
        assertThat(display).linesEqual("""
            |____________ ____________
            |____________ Serenity____
            |____________ ------------
            |Take my love Take my land
            |------------ <rstatus>___
            |____________ ____________
        """.trimMargin())

        tabpage.focusUp()
        assertThat(display).linesEqual("""
            |____________ ____________
            |____________ Serenity____
            |____________ (trstatus)__
            |Take my love Take my land
            |------------ ------------
            |____________ ____________
        """.trimMargin())

        tabpage.focusDown()
        assertThat(display).linesEqual("""
            |____________ ____________
            |____________ Serenity____
            |____________ ------------
            |Take my love Take my land
            |------------ <rstatus>___
            |____________ ____________
        """.trimMargin())
    }

    @Test fun `Close window from a single split`() {
        primary.appendLine("Take my land")
        primary.updateStatusLine("[status]")
        assertThat(primary).hasHeight(6)

        val buffer = JudoBuffer(IdManager(), EmptyStateMap)
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love")

        // close
        tabpage.close(window)

        assertThat(primary).hasHeight(6)
        assertThat(display).linesEqual("""
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

        val buffer = JudoBuffer(IdManager(), EmptyStateMap)
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love")

        assertThat(window).hasHeight(3)
        assertThat(primary).hasHeight(3)
        tabpage.currentWindow = primary

        assertThat(display).linesEqual("""
            |____________
            |Take my love
            |------------
            |____________
            |Take my land
            |[status]____
            |____________
        """.trimMargin())

        tabpage.unsplit()
        assertThat(display).linesEqual("""
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
        assertThat(display).linesEqual("""
            |____________
            |____________
            |____________
            |____________
            |Take my land
            |[status]____
            |____________
        """.trimMargin())
    }

    @Test fun `Resize a vertical stack`() {
        renderer.forceResize(tabpage.width, 9)
        assertThat(tabpage).hasHeight(8)
        assertThat(primary).hasHeight(8)
        tabpage.resize(tabpage.width, 8) // should not be a problem

        val buffer = JudoBuffer(IdManager(), EmptyStateMap)
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love")

        assertThat(window).hasHeight(3)
        assertThat(primary).hasHeight(5)

        window.resize(window.width, 4)

        assertThat(window).hasHeight(4)
        assertThat(primary).hasHeight(4)
    }

    @Test fun `Resize a horizontal stack`() {
        renderer.forceResize(20, tabpage.height)
        assertThat(primary).hasWidth(20)

        val buffer = JudoBuffer(IdManager(), EmptyStateMap)
        val window = tabpage.vsplit(2, buffer)
        window.appendLine("Take my love")

        assertThat(window).hasWidth(2)
        assertThat(primary).hasWidth(17)

        window.resize(8, window.height)

        assertThat(window).hasWidth(8)
        assertThat(primary).hasWidth(11)
    }

    @Test fun `Re-split`() {
        primary.appendLine("Take my land")
        primary.updateStatusLine("[status]")

        val buffer = JudoBuffer(IdManager(), EmptyStateMap)
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love")

        assertThat(window).hasHeight(3)
        assertThat(primary).hasHeight(3)

        // make sure we're good and unsplit
        tabpage.unsplit()
        tabpage.unsplit() // this should be idempotent

        // now, re-split without erroring
        tabpage.hsplit(2, buffer)
        tabpage.currentWindow = primary
        assertThat(display).linesEqual("""
            |_          _
            |Take my love
            |------------
            |_          _
            |Take my land
            |[status]____
            |_          _
        """.trimMargin())
    }

    @Test fun `Restore focus properly after unsplit()`() {
        primary.appendLine("Take my land")
        primary.updateStatusLine("[status]")

        val buffer = JudoBuffer(IdManager(), EmptyStateMap)
        val window = tabpage.hsplit(2, buffer)
        window.appendLine("Take my love")

        assertThat(window).hasHeight(3)
        assertThat(primary).hasHeight(3)

        // make sure we're good and unsplit
        tabpage.unsplit()
        tabpage.unsplit() // this should be idempotent

        // now, re-split without erroring
        assertThat(tabpage.currentWindow).isSameAs(primary)
        assertThat(display).linesEqual("""
            |_          _
            |           _
            |           _
            |_          _
            |Take my land
            |[status]____
            |_          _
        """.trimMargin())
    }

    @Test fun `Vertical window focus commands`() {
        val bottom = tabpage.currentWindow

        val b1 = emptyBuffer()
        val b2 = emptyBuffer()
        val middle = tabpage.hsplit(0.5f, b1)

        tabpage.currentWindow = middle
        val top = tabpage.hsplit(0.5f, b2)

        tabpage.focusUp()
        assertThat(tabpage.currentWindow).all {
            isFocused()
            isSameAs(top)
        }

        tabpage.focusDown(2)
        assertThat(tabpage.currentWindow).all {
            isFocused()
            isSameAs(bottom)
        }

        tabpage.focusUp(2)
        assertThat(tabpage.currentWindow).all {
            isFocused()
            isSameAs(top)
        }

        tabpage.focusDown()
        assertThat(tabpage.currentWindow).all {
            isFocused()
            isSameAs(middle)
        }

        tabpage.focusDown()
        assertThat(tabpage.currentWindow).all {
            isFocused()
            isSameAs(bottom)
        }
    }

    @Test fun `Clear buffer of scrolled window`() {
        primary.appendLine("Take my love take my land")
        primary.updateStatusLine("[status]")
        primary.scrollLines(1)
        assertThat(display).linesEqual("""
            |____________
            |____________
            |____________
            |____________
            |Take my love
            |[status]____
            |____________
        """.trimMargin())

        primary.currentBuffer.clear()
        assertThat(display).linesEqual("""
            |____________
            |____________
            |____________
            |____________
            |____________
            |[status]____
            |____________
        """.trimMargin())

        primary.appendLine("Take my land take me where")
        assertThat(display).linesEqual("""
            |____________
            |____________
            |Take my land
            |take me ____
            |where_______
            |[status]____
            |____________
        """.trimMargin())
    }
}

private fun PrimaryJudoWindow.updateStatusLine(line: String) {
    updateStatusLine(FlavorableStringBuilder.withDefaultFlavor(line))
}
