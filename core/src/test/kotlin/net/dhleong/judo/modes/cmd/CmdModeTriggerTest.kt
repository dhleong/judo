package net.dhleong.judo.modes.cmd

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.runBlocking
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.flavor.Flavor
import net.dhleong.judo.render.flavor.flavor
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.trigger.TriggerManager
import net.dhleong.judo.util.ansi
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeTriggerTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

    @Test fun `trigger() basic`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |def handleTrigger(): print("awesome")
                |trigger('cool', handleTrigger)
            """.trimMargin()

            SupportedScriptTypes.JS -> """
                trigger('cool', function handleTrigger() {
                    print("awesome");
                });
            """.trimIndent()
        })
        assertThat(judo.triggers.hasTriggerFor("cool")).isTrue()

        judo.triggers.process("this is cool")
        assertThat(judo.prints).containsExactly("awesome")
    }

    @Test fun `trigger() as decorator`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@trigger('cool')
                |def handleTrigger(): print("awesome")
            """.trimMargin()

            SupportedScriptTypes.JS -> return@runBlocking
        })
        assertThat(judo.triggers.hasTriggerFor("cool")).isTrue()

        judo.triggers.process("this is cool")
        assertThat(judo.prints).containsExactly("awesome")
    }

    @Test fun `trigger() with regex`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
            import re
            @trigger(re.compile('cool(.*)'))
            def handleTrigger(thing): print("awesome%s" % thing)
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                trigger(/cool(.*)/, function(thing) {
                    print("awesome" + thing);
                });
            """.trimIndent()
        })

        judo.triggers.process("cool story bro")
        assertThat(judo.prints).containsExactly("awesome story bro")
    }

    @Test fun `trigger() strips color by default`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@trigger('cool $1')
                |def handleTrigger(thing): print("awesome %s" % thing)
            """.trimMargin()

            SupportedScriptTypes.JS -> """
                trigger('cool $1', function(thing) {
                    print("awesome " + thing);
                });
            """.trimIndent()
        })

        judo.triggers.process(`cool story`())
        assertThat(judo.prints).containsExactly("awesome story")
    }

    @Test fun `trigger(_, 'color') preserves ANSI`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@trigger('cool $1', 'color')
                |def handleTrigger(thing): print("awesome %s" % thing)
            """.trimMargin()

            SupportedScriptTypes.JS -> """
                trigger('cool $1', 'color', function(thing) {
                    print("awesome " + thing);
                });
            """.trimIndent()
        })

        judo.triggers.process(`cool story`())
        assertThat(judo.prints).hasSize(1)
        assertThat(judo.prints[0] as String).isEqualTo(
            "awesome ${ansi(1,2)}st${ansi(fg=3)}or${ansi(fg=4)}y${ansi(0)}")
    }

    @Test fun `trigger() ending in dot works`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@trigger('cool.')
                |def handleTrigger(): print("awesome.")
            """.trimMargin()

            SupportedScriptTypes.JS -> """
                trigger('cool.', function() {
                    print("awesome.");
                });
            """.trimIndent()
        })
        assertThat(judo.triggers.hasTriggerFor("cool.")).isTrue()

        judo.triggers.process("cool.")
        assertThat(judo.prints).containsExactly("awesome.")
    }

    @Test fun `trigger() supports multiple decorators`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@trigger('cool')
                |@trigger('shiny')
                |def handleTrigger(): print("awesome")
            """.trimMargin()

            // no decorator support:
            SupportedScriptTypes.JS -> return@runBlocking
        })
        assertThat(judo.triggers.hasTriggerFor("cool")).isTrue()
        assertThat(judo.triggers.hasTriggerFor("shiny")).isTrue()

        judo.triggers.process("this is cool")
        assertThat(judo.prints).containsExactly("awesome")

        judo.triggers.process("this is shiny")
        assertThat(judo.prints).containsExactly("awesome", "awesome")
    }

    @Test fun `trigger() and alias() decorators stack`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@trigger('cool')
                |@alias('shiny')
                |def handleTriggerOrAlias(): print("awesome")
            """.trimMargin()

            // no decorators
            SupportedScriptTypes.JS -> return@runBlocking
        })

        assertThat(judo.triggers.hasTriggerFor("cool")).isTrue()
        assertThat(judo.triggers.hasTriggerFor("shiny")).isFalse()
        assertThat(judo.aliases.hasAliasFor("cool")).isFalse()
        assertThat(judo.aliases.hasAliasFor("shiny")).isTrue()

        judo.triggers.process("this is cool")
        assertThat(judo.prints).containsExactly("awesome")

        judo.submit("shiny", fromMap = false)
        assertThat(judo.sends).isEmpty()
        assertThat(judo.prints).containsExactly("awesome", "awesome")
    }

    @Test fun `trigger() only matches complete lines`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@trigger('cool $1')
                |def handleTrigger(thing): print("awesome %s" % thing)
            """.trimMargin()

            SupportedScriptTypes.JS -> """
                trigger('cool $1', function(thing) {
                    print("awesome " + thing);
                });
            """.trimIndent()
        })

        judo.triggers.process(`cool story`().apply {
            removeTrailingNewline()
        })
        judo.triggers.process(`cool story`().apply {
            removeTrailingNewline()
            append("bro\n")
        })
        assertThat(judo.prints).containsExactly("awesome storybro")
    }

    private fun `cool story`() = FlavorableStringBuilder(64).apply {
        append("cool ", Flavor.default)
        append("st", flavor(
            isBold = true,
            foreground = JudoColor.Simple.from(2)
        ))
        append("or", flavor(
            isBold = true,
            foreground = JudoColor.Simple.from(3)
        ))
        append("y", flavor(
            isBold = true,
            foreground = JudoColor.Simple.from(4)
        ))
        append("\n")
    }
}

fun TriggerManager.process(string: String) = process(FlavorableStringBuilder.withDefaultFlavor(
    string + "\n"
))