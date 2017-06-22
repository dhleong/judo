package net.dhleong.judo.modes

import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.complete.DumbCompletionSource
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.util.InputHistory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class PythonCmdModeTest {

    val judo = TestableJudoCore()
    val input = InputBuffer()
    val mode = PythonCmdMode(
        judo, input,
        TestableJudoRenderer(),
        InputHistory(input),
        DumbCompletionSource()
    )

    @Before fun setUp() {
        mode.onEnter()
        judo.clearTestable()
    }

    @Test fun echo() {
        mode.execute("echo('test', 2)")

        assertThat(judo.echos).containsExactly("test", 2)
    }

    @Test fun globals() {
        mode.execute("echo(MYJUDORC)")

        assertThat(judo.echos).containsExactly(USER_CONFIG_FILE.absolutePath)
    }

    @Test fun send_ignoreExtraArgs() {
        mode.execute("send('test', 2)")

        assertThat(judo.sends).containsExactly("test")
    }

    @Test fun map_nnoremap() {
        mode.execute("nnoremap('a', 'bc')")

        assertThat(judo.maps)
            .containsExactly(arrayOf("normal", "a", "bc", false))
    }

    @Test fun map_createMap() {
        mode.execute("createMap('custom', 'a', 'bc', True)")

        assertThat(judo.maps)
            .containsExactly(arrayOf("custom", "a", "bc", true))
    }

    @Test fun alias_static() {
        mode.execute("alias('fun', 'fancy')")

        assertThat(judo.aliases.hasAliasFor("fun")).isTrue()
    }

    @Test fun alias_fun() {
        mode.execute("""
            |def handleAlias(): return "awesome"
            |alias('cool', handleAlias)
            """.trimMargin())

        assertThat(judo.aliases.hasAliasFor("cool")).isTrue()
        assertThat(judo.aliases.process("this is cool").toString())
            .isEqualTo("this is awesome")
    }

    @Test fun alias_funDecorator() {
        mode.execute("""
            |@alias('cool')
            |def handleAlias(): return "awesome"
            """.trimMargin())

        assertThat(judo.aliases.hasAliasFor("cool")).isTrue()
        assertThat(judo.aliases.process("this is cool").toString())
            .isEqualTo("this is awesome")
    }

    @Test fun alias_multiDecorator() {
        mode.execute("""
            |@alias('cool')
            |@alias('shiny')
            |def handleAlias(): return "awesome"
            """.trimMargin())

        assertThat(judo.aliases.hasAliasFor("cool")).isTrue()
        assertThat(judo.aliases.hasAliasFor("shiny")).isTrue()

        assertThat(judo.aliases.process("this is cool").toString())
            .isEqualTo("this is awesome")
        assertThat(judo.aliases.process("this is shiny").toString())
            .isEqualTo("this is awesome")
    }

    @Test fun alias_returnNothing() {
        mode.execute("""
            |@alias('cool')
            |def handleAlias(): pass
            """.trimMargin())

        assertThat(judo.aliases.hasAliasFor("cool")).isTrue()
        assertThat(judo.aliases.process("cool").toString())
            .isEmpty()
    }

    @Test fun trigger() {
        mode.execute("""
            |def handleTrigger(): echo("awesome")
            |trigger('cool', handleTrigger)
            """.trimMargin())

        assertThat(judo.triggers.hasTriggerFor("cool")).isTrue()
        judo.triggers.process("this is cool")
        assertThat(judo.echos).containsExactly("awesome")
    }

    @Test fun trigger_decorator() {
        mode.execute("""
            |@trigger('cool')
            |def handleTrigger(): echo("awesome")
            """.trimMargin())

        assertThat(judo.triggers.hasTriggerFor("cool")).isTrue()
        judo.triggers.process("this is cool")
        assertThat(judo.echos).containsExactly("awesome")
    }

    @Test fun trigger_multiDecorator() {
        mode.execute("""
            |@trigger('cool')
            |@trigger('shiny')
            |def handleTrigger(): echo("awesome")
            """.trimMargin())

        assertThat(judo.triggers.hasTriggerFor("cool")).isTrue()
        assertThat(judo.triggers.hasTriggerFor("shiny")).isTrue()

        judo.triggers.process("this is cool")
        assertThat(judo.echos).containsExactly("awesome")

        judo.triggers.process("this is shiny")
        assertThat(judo.echos).containsExactly("awesome", "awesome")
    }

    @Test fun triggerAndAlias() {
        mode.execute("""
            |@trigger('cool')
            |@alias('shiny')
            |def handleTriggerOrAlias(): echo("awesome")
            """.trimMargin())

        assertThat(judo.triggers.hasTriggerFor("cool")).isTrue()
        assertThat(judo.triggers.hasTriggerFor("shiny")).isFalse()
        assertThat(judo.aliases.hasAliasFor("cool")).isFalse()
        assertThat(judo.aliases.hasAliasFor("shiny")).isTrue()

        judo.triggers.process("this is cool")
        assertThat(judo.echos).containsExactly("awesome")

        judo.send("shiny", fromMap = false)
        assertThat(judo.sends).isEmpty()
        assertThat(judo.echos).containsExactly("awesome", "awesome")
    }

    @Test fun prompt() {

        mode.execute("""
            |prompt('^Input($1)', 'prompt $1>')
            """.trimMargin())

        assertThat(judo.prompts.size).isEqualTo(1)
        var lastPrompt: String? = null
        val result = judo.prompts.process("Input(42)", { _, prompt ->
            lastPrompt = prompt
        })
        assertThat(result).isEmpty()
        assertThat(lastPrompt).isEqualToIgnoringCase("prompt 42>")
    }

    @Test fun sharedVars() {

        mode.execute("""
            |value = "magic"
            """.trimMargin())

        mode.execute("""
            |echo(value)
            """.trimMargin())

        assertThat(judo.echos)
            .containsExactly("magic")
    }

    @Test fun dontOverwiteBuiltins() {
        mode.execute("def echo(): pass")

        mode.execute("""
            |echo("magic")
            """.trimMargin())

        assertThat(judo.echos)
            .containsExactly("magic")
    }
}

