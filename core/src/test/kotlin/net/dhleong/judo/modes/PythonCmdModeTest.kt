package net.dhleong.judo.modes

import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.assertThat
import net.dhleong.judo.complete.DumbCompletionSource
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.util.InputHistory
import net.dhleong.judo.util.ansi
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

/**
 * @author dhleong
 */
class PythonCmdModeTest {

    val judo = TestableJudoCore()
    val input = InputBuffer()
    val mode = PythonCmdMode(
        judo, IdManager(), input,
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

    @Test fun alias_regex() {
        mode.execute("""
            |import re
            |@alias(re.compile('^cool(.*)'))
            |def handleAlias(arg): echo(arg.strip())
            """.trimMargin())

        assertThat(judo.aliases.process("cool cool story bro").toString())
            .isEmpty()
        assertThat(judo.echos).containsExactly("cool story bro")

        // the regex should match nothing
        assertThat(judo.aliases.process("cool").toString())
            .isEmpty()
        assertThat(judo.echos).containsExactly("cool story bro", "")
    }

    @Test fun alias_pyOnlyRegex() {
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

        assertThat(judo.aliases.process("cool cool story bro").toString())
            .isEmpty()
        assertThat(judo.echos).containsExactly("cool story bro")
    }

    @Test fun event_fun() {
        mode.execute("""
            |def handleEvent(): echo("awesome")
            |event('cool', handleEvent)
            """.trimMargin())

        assertThat(judo.events.has("cool")).isTrue()

        judo.events.raise("cool", "cold")
        assertThat(judo.echos)
            .containsExactly("awesome")
    }

    @Test fun event_destructure() {
        mode.execute("""
            @event("cool")
            def handleEvent(key, val):
                echo("awesome %s:%s" % (key, val))
            """.trimIndent())

        assertThat(judo.events.has("cool")).isTrue()

        judo.events.raise("cool", arrayOf("cold", "colder"))
        assertThat(judo.echos)
            .containsExactly("awesome cold:colder")
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

    @Test fun trigger_regex() {
        mode.execute("""
            import re
            @trigger(re.compile('cool(.*)'))
            def handleTrigger(thing): echo("awesome%s" % thing)
            """.trimIndent())

        judo.triggers.process("cool story bro")
        assertThat(judo.echos).containsExactly("awesome story bro")
    }

    @Test fun trigger_stripColor() {
        mode.execute("""
            |@trigger('cool $1')
            |def handleTrigger(thing): echo("awesome %s" % thing)
            """.trimMargin())

        judo.triggers.process("cool ${ansi(1,2)}st${ansi(1,3)}or${ansi(1,4)}y")
        assertThat(judo.echos).containsExactly("awesome story")
    }

    @Test fun trigger_keepColor() {
        mode.execute("""
            |@trigger('cool $1', 'color')
            |def handleTrigger(thing): echo("awesome %s" % thing)
            """.trimMargin())

        judo.triggers.process("cool ${ansi(1,2)}st${ansi(1,3)}or${ansi(1,4)}y")
        assertThat(judo.echos).hasSize(1)
        assertThat(judo.echos[0] as String).isEqualTo(
            "awesome ${ansi(1,2)}st${ansi(fg=3)}or${ansi(fg=4)}y${ansi(0)}")
    }

    @Test fun trigger_withDot() {
        mode.execute("""
            |@trigger('cool.')
            |def handleTrigger(): echo("awesome.")
            """.trimMargin())

        assertThat(judo.triggers.hasTriggerFor("cool.")).isTrue()
        judo.triggers.process("cool.")
        assertThat(judo.echos).containsExactly("awesome.")
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

    @Test fun settings() {
        assertThat(WORD_WRAP !in judo.state).isTrue()

        assertThatThrownBy {
            mode.execute("set('nonsense', True)")
        }.hasMessageContaining("No such setting")

        assertThatThrownBy {
            mode.execute("set('wordwrap', 'string?')")
        }.hasMessageContaining("is invalid for")

        mode.execute("set('wordwrap', True)")

        assertThat(judo.state[WORD_WRAP]).isEqualTo(true)
    }

    @Test fun echoSettings() {
        assertThat(WORD_WRAP !in judo.state).isTrue()

        mode.execute("set('wordwrap')")
        assertThat(judo.echos).containsExactly("wordwrap = true (default)")
        judo.echos.clear()

        mode.execute("set('wordwrap', False)")
        assertThat(judo.echos).containsExactly("wordwrap = false")
    }

    @Test fun dontOverwiteBuiltins() {
        mode.execute("def echo(): pass")

        mode.execute("""
            |echo("magic")
            """.trimMargin())

        assertThat(judo.echos)
            .containsExactly("magic")
    }

    @Test fun hsplit() {
        mode.execute("""
            newWin = hsplit(20)
            echo(newWin.id)
            echo(newWin.buffer)
            newWin.buffer.append("test")
            """.trimIndent())

        assertThat(judo.echos).isNotEmpty()
        val window = judo.tabpage.findWindowById(judo.echos[0] as Int)
        assertThat(window).isNotNull

        val buffer = window!!.currentBuffer
        assertThat(buffer[0].toString())
            .isEqualTo("test")

        judo.echos.clear()
        mode.execute("""
            newWin.buffer.set(['Foo', 'Bar'])
            echo(len(newWin.buffer))
            """.trimIndent())

        assertThat(buffer[0].toString())
            .isEqualTo("Foo")
        assertThat(buffer[1].toString())
            .isEqualTo("Bar")
        assertThat(buffer.size).isEqualTo(2)
        assertThat(judo.echos).containsExactly(2)
    }

    @Test fun hsplit_resize() {
        mode.execute("""
            newWin = hsplit(20)
            echo(newWin.id)
            echo(newWin.buffer)
            newWin.buffer.append("test")
            newWin.resize(newWin.width, 4)
            """.trimIndent())

        assertThat(judo.echos).isNotEmpty()
        val window = judo.tabpage.findWindowById(judo.echos[0] as Int)!!

        assertThat(window).hasHeight(4)

        val primary = judo.tabpage.currentWindow
        assertThat(primary)
            .isNotSameAs(window)
            .hasHeight(judo.tabpage.height - 4 - 1) // -1 for the separator!
    }

    @Test fun readFileClearsOld() {
        mode.readLikeFile("test.py", """
            alias('shiny $1', 'Mighty fine $1')

            @event('CONNECTED')
            def on_connect(): pass

            nmap('gkf', 'ikeep flyin<cr>')
            prompt('foo $1>', '[$1]')

            @trigger('foo')
            def on_foo(): pass
            """.trimIndent())

        assertThat(judo.aliases.hasAliasFor("shiny $1")).isTrue()
        assertThat(judo.events.has("CONNECTED")).isTrue()
        assertThat(judo.events.has("DISCONNECTED")).isFalse()
        assertThat(judo.prompts.size).isEqualTo(1)
        assertThat(judo.maps).contains(arrayOf("normal", "gkf", "ikeep flyin<cr>", true))
        assertThat(judo.triggers.hasTriggerFor("foo")).isTrue()

        // read a different file, and no change to original
        mode.readLikeFile("different.py", """
            @event("DISCONNECTED")
            def on_disconnect(): pass
            """.trimIndent())
        assertThat(judo.aliases.hasAliasFor("shiny $1")).isTrue()
        assertThat(judo.events.has("CONNECTED")).isTrue()
        assertThat(judo.events.has("DISCONNECTED")).isTrue()
        assertThat(judo.prompts.size).isEqualTo(1)
        assertThat(judo.maps).contains(arrayOf("normal", "gkf", "ikeep flyin<cr>", true))
        assertThat(judo.triggers.hasTriggerFor("foo")).isTrue()

        // read the original file (which is now inexplicably empty) and
        // clear out the things it created...
        mode.readLikeFile("test.py","")
        assertThat(judo.aliases.hasAliasFor("shiny $1")).isFalse()
        assertThat(judo.events.has("CONNECTED")).isFalse()
        assertThat(judo.maps).isEmpty()
        assertThat(judo.prompts.size).isEqualTo(0)
        assertThat(judo.triggers.hasTriggerFor("foo")).isFalse()

        // ... but the other file is intact
        assertThat(judo.events.has("DISCONNECTED")).isTrue()
    }
}

private fun PythonCmdMode.readLikeFile(fileName: String, fileContents: String) =
    readFile(File(fileName), ByteArrayInputStream(fileContents.toByteArray()))

