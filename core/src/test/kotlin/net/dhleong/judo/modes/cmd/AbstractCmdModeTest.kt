package net.dhleong.judo.modes.cmd

import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.complete.DumbCompletionSource
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.modes.CmdMode
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.script.JreJsScriptingEngine
import net.dhleong.judo.script.JythonScriptingEngine
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.util.InputHistory
import org.junit.Before
import org.junit.runners.Parameterized
import java.io.File

enum class SupportedScriptTypes {
    JS,
    PY
}

/**
 * @author dhleong
 */
abstract class AbstractCmdModeTest(
    protected val factory: ScriptingEngine.Factory
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}. {0}")
        fun data() = listOf<Array<Any>>(
            arrayOf(JreJsScriptingEngine.Factory()),
            arrayOf(JythonScriptingEngine.Factory())
        )
    }

    protected val judo = TestableJudoCore()
    protected val input = InputBuffer()
    protected val renderer = TestableJudoRenderer()
    protected val mode = CmdMode(
        judo, IdManager(), input,
        renderer,
        InputHistory(input),
        DumbCompletionSource(),
        File(".judo"),
        File(".judo/init.${scriptType().toString().toLowerCase()}"),
        factory
    )

    @Before fun setUp() {
        mode.onEnter()
        judo.clearTestable()
    }

    /*
     * Utils
     */

    private fun testingType(fileExtension: String) =
        factory.supportsFileType(fileExtension)

    protected fun scriptType() = when {
        testingType("js") -> SupportedScriptTypes.JS
        testingType("py") -> SupportedScriptTypes.PY
        else -> throw UnsupportedOperationException()
    }

    protected fun fnCall(fnName: String, vararg args: Any): String =
        factory.formatFnCall(fnName, *args.map {
            when (it) {
                is String -> "\"$it\""

                is Boolean -> when (scriptType()) {
                    SupportedScriptTypes.PY -> it.toString().capitalize()
                    else -> it.toString()
                }

                else -> it.toString()
            }
        }.toTypedArray())

    protected class Var(private val fnName: String) {
        override fun toString() = fnName
    }

}