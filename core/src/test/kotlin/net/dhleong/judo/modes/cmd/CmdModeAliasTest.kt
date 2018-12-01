package net.dhleong.judo.modes.cmd

import assertk.assert
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import net.dhleong.judo.script.ScriptingEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeAliasTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

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
            |${fnCall("alias",
            Var("regex"),
            Var("handleAlias")
        )}
        """.trimMargin())

        assert(judo.aliases.process("keep flyin'").toString())
            .isEmpty()
        assert(judo.echos).containsExactly("flyin'")

    }
}

