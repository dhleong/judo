package net.dhleong.judo.jline

import assertk.all
import assertk.assert
import assertk.assertions.each
import assertk.assertions.hasLength
import assertk.assertions.hasSize
import com.nhaarman.mockito_kotlin.mock
import net.dhleong.judo.DummyConnectionFactory
import net.dhleong.judo.JudoCore
import net.dhleong.judo.StateMap
import net.dhleong.judo.assertionsWhileTyping
import net.dhleong.judo.bufferOf
import net.dhleong.judo.cmdMode
import net.dhleong.judo.emptyBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.parseAnsi
import net.dhleong.judo.render.toFlavorable
import net.dhleong.judo.script.JavaRegexPatternSpec
import net.dhleong.judo.util.PatternProcessingFlags
import net.dhleong.judo.util.ansi
import net.dhleong.judo.yieldKeys
import org.junit.Before
import org.junit.Test
import java.util.EnumSet
import java.util.regex.Pattern

/**
 * @author dhleong
 */
class JudoCoreJLineIntegrationTest {

    lateinit var display: JLineDisplay
    lateinit var settings: StateMap

    lateinit var renderer: JLineRenderer
    private lateinit var judo: JudoCore

    @Before fun setUp() {
        val width = 20
        val height = 4

        settings = StateMap()
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

        assert(display.toAttributedStrings()).all {
            hasSize(4)
            each {
                it.hasLength(10)
            }
        }
    }

    @Test fun `Handle terminal height resize`() {
        renderer.forceResize(20, 8)
        renderer.forceResize(20, 2)
        assert(display).linesEqual("""
            |____________[NORMAL]
            |____________________
        """.trimMargin())

        assert(display.toAttributedStrings()).all {
            hasSize(2)
            each {
                it.hasLength(20)
            }
        }
    }

    @Test fun `Multiline echo() from script`() {
        renderer.forceResize(30, 4)
        judo.feedKeys(":echo(\"mal\\nreynolds\")<cr>")
        assert(display).linesEqual("""
            |mal___________________________
            |reynolds______________________
            |Press ENTER or type command to
            |continue______________________
        """.trimMargin())
    }

    @Test fun `Window command mappings`() {
        renderer.forceResize(10, 6)

        val buffer = bufferOf("""
            mreynolds
        """.trimIndent())
        val win = renderer.currentTabpage.hsplit(2, buffer)
        win.updateStatusLine("[status]".toFlavorable())

        assert(display).linesEqual("""
            |__________
            |mreynolds_
            |[status]__
            |__________
            |----------
            |__________
        """.trimMargin())

        judo.feedKeys("<ctrl-w>j")
        assert(display).linesEqual("""
            |__________
            |mreynolds_
            |----------
            |__________
            |__[NORMAL]
            |__________
        """.trimMargin())

        // over-count
        judo.feedKeys("2<ctrl-w>k")
        assert(display).linesEqual("""
            |__________
            |mreynolds_
            |[status]__
            |__________
            |----------
            |__________
        """.trimMargin())
    }

    @Test fun `Send commands to the active window`() {
        renderer.forceResize(10, 6)

        val buffer = bufferOf("""
            Take me where I cannot stand
        """.trimIndent())
        val win = renderer.currentTabpage.hsplit(2, buffer)
        win.updateStatusLine("[status]".toFlavorable())

        assert(display).linesEqual("""
            |cannot____
            |stand_____
            |[status]__
            |__________
            |----------
            |__________
        """.trimMargin())

        judo.feedKeys("<ctrl-f>")
        assert(display).linesEqual("""
            |Take me___
            |where I___
            |[status]__
            |__________
            |----------
            |__________
        """.trimMargin())
    }

    @Test fun `Print mappings`() {
        renderer.forceResize(15, 5)
        judo.printMappings("normal")

        // NOTE: no mappings to print; this is to ensure
        // that we print without error
        assert(display).linesEqual("""
            |_______________
            |KeyMappings____
            |===========____
            |_______[NORMAL]
            |_______________
        """.trimMargin())
    }

    @Test fun `Mode indicator in vsplit window`() {
        renderer.forceResize(20, 5)
        renderer.currentTabpage.vsplit(8, emptyBuffer())

        // NOTE: no mappings to print; this is to ensure
        // that we print without error
        assert(display).linesEqual("""
            |___________ ________
            |___________ ________
            |___________ ________
            |----------- [NORMAL]
            |___________ ________
        """.trimMargin())
    }

    @Test fun `Render prompts`() {
        judo.prompts.define("^HP: $1", "HP $1")
        judo.onIncomingBuffer("HP: 42".toFlavorable())
        assert(display).linesEqual("""
            |____________________
            |____________________
            |HP 42_______[NORMAL]
            |____________________
        """.trimMargin())
    }

    @Test fun `Render ANSI prompts`() {
        val pattern = "^(HP.*)$"
        judo.prompts.define(JavaRegexPatternSpec(
            pattern,
            Pattern.compile(pattern),
            flags = EnumSet.of(PatternProcessingFlags.KEEP_COLOR)
        ), "$1")

        val input = "\u001b[35mHP: \u001b[36m42"
        judo.onIncomingBuffer(input.parseAnsi())
        assert(display).ansiLinesEqual("""
            |____________________
            |____________________
            |$input${ansi(0)}______[NORMAL]
            |____________________
        """.trimMargin())
    }

    @Test fun `Render while typing in input()`() = assertionsWhileTyping {
        yieldKeys(":print(input(\"input:\"))<cr>")

        assert(display).linesEqual("""
            |____________________
            |____________________
            |_____________[INPUT]
            |input:______________
        """.trimMargin())

        yieldKeys("test")

        assert(display).linesEqual("""
            |____________________
            |____________________
            |_____________[INPUT]
            |input:test__________
        """.trimMargin())
    }

    @Test fun `Redraw clears blocking echo`() {
        judo.cmdMode.execute("""
            def echoAndClear():
                echo("Take my love")
                echo("Take my land")
        """.trimIndent())

        judo.feedKeys(":echoAndClear()<cr>")
        assert(display).linesEqual("""
            |Take my love________
            |Take my land________
            |Press ENTER or type_
            |command to continue_
        """.trimMargin())

        judo.cmdMode.execute("redraw()")
        assert(display).linesEqual("""
            |____________________
            |____________________
            |____________[NORMAL]
            |____________________
        """.trimMargin())
    }

    @Test fun `Immediate redraw() clears blocking echo`() {
        judo.cmdMode.execute("""
            def echoAndClear():
                echo("Take my love")
                echo("Take my land")
                redraw()
        """.trimIndent())

        judo.feedKeys(":echoAndClear()<cr>")
        assert(display).linesEqual("""
            |____________________
            |____________________
            |____________[NORMAL]
            |____________________
        """.trimMargin())
    }

    @Test fun `Command Line Mode from input()`() = assertionsWhileTyping {
        yieldKeys(":print(input(\"m:\"))<cr>rey<ctrl-f>Inolds")

        assert(display).linesEqual("""
            |____________________
            |____________________
            |_________[CL:INSERT]
            |@noldsrey___________
        """.trimMargin())

        yieldKeys("<cr>")
        assert(display).linesEqual("""
            |____________________
            |noldsrey____________
            |____________[NORMAL]
            |____________________
        """.trimMargin())
    }

    @Test fun `Exiting Command Line Mode hides prompt`() = assertionsWhileTyping {
        yieldKeys(":mrey<ctrl-f>")

        assert(display).linesEqual("""
            |____________________
            |____________________
            |_________[CL:NORMAL]
            |:mrey_______________
        """.trimMargin())

        yieldKeys("<ctrl-c>")
        assert(display).linesEqual("""
            |____________________
            |____________________
            |:mrey_______________
            |____________________
        """.trimMargin())
    }

    @Test(timeout = 3000) fun `Hitting ESC in Command Line Mode doesn't result in hang on submit`() = assertionsWhileTyping {
        yieldKeys(":print(\"hi\")<ctrl-f>A<esc>")

        assert(display).linesEqual("""
            |____________________
            |____________________
            |_________[CL:NORMAL]
            |:print("hi")________
        """.trimMargin())

        yieldKeys("<cr>")
        assert(display).linesEqual("""
            |____________________
            |hi__________________
            |____________[NORMAL]
            |____________________
        """.trimMargin())
    }

    private inline fun assertionsWhileTyping(
        crossinline block: suspend SequenceScope<Key>.() -> Unit
    ) = assertionsWhileTyping(judo, block)

}

