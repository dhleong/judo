package net.dhleong.judo.modes

import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.complete.DumbCompletionSource
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.script.JreJsScriptingEngine
import net.dhleong.judo.util.InputHistory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class BaseCmdModeTest {

    private val judo = TestableJudoCore()
    private val input = InputBuffer()
    private val renderer = TestableJudoRenderer()
    private val mode = CmdMode(
        judo, IdManager(), input,
        renderer,
        InputHistory(input),
        DumbCompletionSource(),
        JreJsScriptingEngine.Factory()
    )

    @Before fun setUp() {
        mode.onEnter()
        judo.clearTestable()
    }

    @Test fun helpColumns() {
        val colWidth = "createUserMode  ".length
        renderer.settableWindowWidth = colWidth * 2

        mode.showHelp()

        assertThat(judo.echos)
            .startsWith("alias           cmap            ")

    }
}