package net.dhleong.judo.modes

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isNotNull
import assertk.assertions.message
import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.complete.DumbCompletionSource
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.script.Jsr223ScriptingEngine
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.util.InputHistory
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeTest(
    scriptEngineFactory: ScriptingEngine.Factory
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf<Array<Any>>(
            arrayOf(Jsr223ScriptingEngine.Factory())
        )
    }

    private val judo = TestableJudoCore()
    private val input = InputBuffer()
    private val mode = CmdMode(
        judo, IdManager(), input,
        TestableJudoRenderer(),
        InputHistory(input),
        DumbCompletionSource(),
        scriptEngineFactory
    )

    @Before fun setUp() {
        mode.onEnter()
        judo.clearTestable()
    }

    @Test fun echo() {
        mode.execute("echo('test', 2)")

        assert(judo.echos).containsExactly("test", 2)
    }

    @Test fun globals() {
        mode.execute("echo(MYJUDORC)")

        assert(judo.echos).containsExactly(USER_CONFIG_FILE.absolutePath)
    }

    @Test fun `Complain with unexpected number of args`() {
        // works:
        mode.execute("send('mreynolds')")
        assert(judo.sends).containsExactly("mreynolds")

        assert {
            mode.execute("send()")
        }.thrownError {
            message().isNotNull {
                it.contains("arguments")
            }
        }

        assert {
            mode.execute("send('niska', 2)")
        }.thrownError {
            message().isNotNull {
                it.contains("arguments")
            }
        }
    }

    @Test fun deleteMap() {
        mode.execute("deleteMap('gf', 'to')")
    }

}