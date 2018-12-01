package net.dhleong.judo.modes

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.message
import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.complete.DumbCompletionSource
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.script.Jsr223ScriptingEngine
import net.dhleong.judo.script.JythonScriptingEngine
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.util.InputHistory
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

enum class SupportedScriptTypes {
    JS,
    PY
}

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeTest(
    private val factory: ScriptingEngine.Factory
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf<Array<Any>>(
            arrayOf(Jsr223ScriptingEngine.Factory()),
            arrayOf(JythonScriptingEngine.Factory())
        )
    }

    private val judo = TestableJudoCore()
    private val input = InputBuffer()
    private val mode = CmdMode(
        judo, IdManager(), input,
        TestableJudoRenderer(),
        InputHistory(input),
        DumbCompletionSource(),
        factory
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
        mode.execute(fnCall("send", "mreynolds"))
        assert(judo.sends).containsExactly("mreynolds")

        assert {
            mode.execute(fnCall("send"))
        }.thrownError {
            message().isNotNull {
                it.contains("arguments")
            }
        }

        assert {
            mode.execute(fnCall("send", "niska", 2))
        }.thrownError {
            message().isNotNull {
                it.contains("arguments")
            }
        }
    }

    @Test fun deleteMap() {
        mode.execute(fnCall("deleteMap", "gf", "to"))
    }

    @Test fun `alias() with function`() {
        val funcDef = when (scriptType()) {
            SupportedScriptTypes.JS -> """
                function handleAlias() {
                    return "awesome"
                }
            """.trimIndent()

            SupportedScriptTypes.PY -> """
                def handleAlias(): return "awesome"
            """.trimIndent()
        }

        mode.execute("""
            $funcDef
            ${fnCall("alias", "cool", Var("handleAlias"))}
            """.trimIndent())

        assert(judo.aliases.hasAliasFor("cool")).isTrue()
        assert(judo.aliases.process("this is cool").toString())
            .isEqualTo("this is awesome")
    }

    @Ignore
    @Test fun `alias() with Regex`() {
        val regex = "^keep(.*)"
        val regexDef = when (scriptType()) {
            SupportedScriptTypes.JS -> """
                var regex = /$regex/;
            """.trimIndent()

            SupportedScriptTypes.PY -> """
                |import re
                |regex = re.compile('$regex')
            """.trimMargin()
        }

        val fnDef = when (scriptType()) {
            SupportedScriptTypes.JS -> """
                function handleAlias(arg) {
                    echo(arg.trim());
                }
            """.trimIndent()

            SupportedScriptTypes.PY -> """
                def handleAlias(arg): echo(arg.strip())
            """.trimIndent()
        }

        mode.execute("""
            |$regexDef
            |$fnDef
            |${fnCall("alias", Var("regex"), Var("handleAlias"))}
        """.trimMargin())

        assert(judo.aliases.process("keep flyin'").toString())
            .isEmpty()
        assert(judo.echos).containsExactly("flyin'")

    }

    /*
     * Utils
     */

    private fun scriptType() = when {
        testingType("js") -> SupportedScriptTypes.JS
        testingType("py") -> SupportedScriptTypes.PY
        else -> throw UnsupportedOperationException()
    }

    private fun testingType(fileExtension: String) =
        factory.supportsFileType(fileExtension)

    private fun fnCall(fnName: String, vararg args: Any): String =
        factory.formatFnCall(fnName, *args.map {
            if (it is String) "\"$it\""
            else it.toString()
        }.toTypedArray())

    private class Var(val fnName: String) {
        override fun toString() = fnName
    }

}