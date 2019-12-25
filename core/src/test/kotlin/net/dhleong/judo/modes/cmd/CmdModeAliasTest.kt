package net.dhleong.judo.modes.cmd

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlinx.coroutines.runBlocking
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

    @Test fun `alias() static`() = runBlocking {
        mode.execute("alias('fun', 'fancy')")

        assertThat(judo.aliases.hasAliasFor("fun")).isTrue()
        assertThat(judo.aliases.process("That was fun").toString())
            .isEqualTo("That was fancy")
    }

    @Test fun `alias() with function`() = runBlocking {
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

        assertThat(judo.aliases.hasAliasFor("cool")).isTrue()
        assertThat(judo.aliases.process("this is cool").toString())
            .isEqualTo("this is awesome")
    }

    @Test fun `alias() as function decorator`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@alias('cool')
                |def handleAlias(): return "awesome"
            """.trimMargin()

            // Most languages, sadly, don't support decorators:
            SupportedScriptTypes.JS -> return@runBlocking
        })

        assertThat(judo.aliases.hasAliasFor("cool")).isTrue()
        assertThat(judo.aliases.process("this is cool").toString())
            .isEqualTo("this is awesome")
    }

    @Test fun `alias() with multiple decorators`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@alias('cool')
                |@alias('shiny')
                |def handleAlias(): return "awesome"
            """.trimMargin()

            // Most languages, sadly, don't support decorators:
            SupportedScriptTypes.JS -> return@runBlocking
        })

        assertThat(judo.aliases.hasAliasFor("cool")).isTrue()
        assertThat(judo.aliases.hasAliasFor("shiny")).isTrue()

        assertThat(judo.aliases.process("this is cool").toString())
            .isEqualTo("this is awesome")
        assertThat(judo.aliases.process("this is shiny").toString())
            .isEqualTo("this is awesome")
    }

    @Test fun `alias() with empty-return function`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@alias('cool')
                |def handleAlias(): pass
            """.trimMargin()

            SupportedScriptTypes.JS -> """
                alias('cool', function handleAlias() {});
            """.trimIndent()
        })

        assertThat(judo.aliases.hasAliasFor("cool")).isTrue()
        assertThat(judo.aliases.process("cool").toString())
            .isEmpty()
    }

    @Test fun `alias() with Regex`() = runBlocking {
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
                    print(arg.trim());
                }
            """.trimIndent()

            SupportedScriptTypes.PY -> """
                def handleAlias(arg): print(arg.strip())
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

        assertThat(judo.aliases.process("keep flyin'").toString())
            .isEmpty()
        assertThat(judo.prints).containsExactly("flyin'")
    }

    @Test fun `alias() with Python-only Regex`() = runBlocking {
        if (scriptType() != SupportedScriptTypes.PY) {
            // skip!
            return@runBlocking
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
            |def handleAlias(arg): print(arg.strip())
            """.trimMargin())

        assertThat(judo.aliases.process("cool cool story bro").toString())
            .isEmpty()
        assertThat(judo.prints).containsExactly("cool story bro")
    }

}

