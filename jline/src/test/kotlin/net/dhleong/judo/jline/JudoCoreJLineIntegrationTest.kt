package net.dhleong.judo.jline

import assertk.assert
import com.nhaarman.mockito_kotlin.mock
import net.dhleong.judo.DummyConnectionFactory
import net.dhleong.judo.JudoCore
import net.dhleong.judo.StateMap
import net.dhleong.judo.render.IdManager
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class JudoCoreJLineIntegrationTest {

    lateinit var display: JLineDisplay
    lateinit var settings: StateMap

    lateinit var renderer: JLineRenderer2
    private lateinit var judo: JudoCore

    @Before fun setUp() {
        val width = 20
        val height = 4

        settings = StateMap()
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
        judo = JudoCore(
            renderer, mock {  },
            settings,
            connections = DummyConnectionFactory
        )
    }

    @Test fun `Basic render`() {
        assert(display).linesEqual("""
            |____________________
            |____________________
            |____________[NORMAL]
            |____________________
        """.trimMargin())
    }

    @Test fun `Handle terminal width resize`() {
        renderer.forceResize(10, 4)
        assert(display).linesEqual("""
            |__________
            |__________
            |__[NORMAL]
            |__________
        """.trimMargin())
    }

    @Test fun `Handle terminal height resize`() {
        renderer.forceResize(20, 2)
        renderer.forceResize(20, 8)
        renderer.forceResize(20, 4)
        renderer.forceResize(20, 2)
        renderer.forceResize(20, 8)
        assert(display).linesEqual("""
            |____________________
            |____________________
            |____________________
            |____________________
            |____________________
            |____________________
            |____________[NORMAL]
            |____________________
        """.trimMargin())
    }
}