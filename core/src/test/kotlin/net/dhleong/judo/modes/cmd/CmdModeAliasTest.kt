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

    @Test fun `alias() static`() {
        mode.execute("alias('fun', 'fancy')")

        assert(judo.aliases.hasAliasFor("fun")).isTrue()
        assert(judo.aliases.process("That was fun").toString())
            .isEqualTo("That was fancy")
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

    @Test fun `alias() as function decorator`() {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@alias('cool')
                |def handleAlias(): return "awesome"
            """.trimMargin()

            // Most languages, sadly, don't support decorators:
            SupportedScriptTypes.JS -> return
        })

        assert(judo.aliases.hasAliasFor("cool")).isTrue()
        assert(judo.aliases.process("this is cool").toString())
            .isEqualTo("this is awesome")
    }

    @Test fun `alias() with multiple decorators`() {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@alias('cool')
                |@alias('shiny')
                |def handleAlias(): return "awesome"
            """.trimMargin()

            // Most languages, sadly, don't support decorators:
            SupportedScriptTypes.JS -> return
        })

        assert(judo.aliases.hasAliasFor("cool")).isTrue()
        assert(judo.aliases.hasAliasFor("shiny")).isTrue()

        assert(judo.aliases.process("this is cool").toString())
            .isEqualTo("this is awesome")
        assert(judo.aliases.process("this is shiny").toString())
            .isEqualTo("this is awesome")
    }

    @Test fun `alias() with empty-return function`() {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@alias('cool')
                |def handleAlias(): pass
            """.trimMargin()

            SupportedScriptTypes.JS -> """
                alias('cool', function handleAlias() {});
            """.trimIndent()
        })

        assert(judo.aliases.hasAliasFor("cool")).isTrue()
        assert(judo.aliases.process("cool").toString())
            .isEmpty()
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

    @Test fun `alias() with Python-only Regex`() {
        if (scriptType() != SupportedScriptTypes.PY) {
            // skip!
            return
        }

        // NOTE: breaking the black box a bit here for thorough testing;
        // when possible, we compile the python regex into Java regex
        // for better efficiency, but some (very few) things in python regex
        // don't translate; in such sad situations, we fallback to delegating
        // to the python regex stuff.
        // The (?L) flag is such a thing
        mode.execute("""
            |import re
            |@alias(re.compile(r'(?L)^cool(.*)'))
            |def handleAlias(arg): echo(arg.strip())
            """.trimMargin())

        assert(judo.aliases.process("cool cool story bro").toString())
            .isEmpty()
        assert(judo.echos).containsExactly("cool story bro")
    }

}

