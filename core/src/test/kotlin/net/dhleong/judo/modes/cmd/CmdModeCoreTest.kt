package net.dhleong.judo.modes.cmd

import assertk.Assert
import assertk.assert
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.message
import assertk.assertions.support.expected
import assertk.assertions.support.show
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.modes.CmdMode
import net.dhleong.judo.modes.ScriptExecutionException
import net.dhleong.judo.net.createURI
import net.dhleong.judo.script.ScriptingEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayInputStream
import java.io.File

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeCoreTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

    @Test fun print() {
        mode.execute("print('test', 2)")

        assert(judo.prints).containsExactly("test", 2)
    }

    @Test fun globals() {
        mode.execute("print(MYJUDORC)")

        assert(judo.prints).containsExactly(mode.userConfigFile.absolutePath)
    }

    @Test fun `var definitions are shared across execute() calls`() {

        mode.execute("""
            value = "magic"
        """.trimIndent())

        mode.execute("""
            print(value)
        """.trimIndent())

        assert(judo.prints).containsExactly("magic")
    }

    @Test fun `Complain with unexpected number of args`() {
        assert {
            mode.execute(fnCall("send", "mreynolds"))
            assert(judo.sends).containsExactly("mreynolds")
        }.doesNotThrowAnyException()

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

    @Test fun settings() {
        assert(WORD_WRAP !in judo.state).isTrue()

        assert {
            mode.execute(fnCall("config", "nonsense", true))
        }.thrownError {
            message().isNotNull {
                it.contains("No such setting")
            }
        }

        assert {
            mode.execute(fnCall("config", "wordwrap", "string?"))
        }.thrownError {
            message().isNotNull {
                it.contains("is invalid for")
            }
        }

        mode.execute(fnCall("config", "wordwrap", true))

        assert(judo.state[WORD_WRAP]).isEqualTo(true)
    }

    @Test fun `config('setting') prints its value`() {
        assert(WORD_WRAP !in judo.state).isTrue()

        mode.execute(fnCall("config", "wordwrap"))
        assert(judo.prints).containsExactly("wordwrap = true (default)")
        judo.prints.clear()

        mode.execute(fnCall("config", "wordwrap", false))
        assert(judo.prints).containsExactly("wordwrap = false")
    }

    @Test fun `Don't allow builtins to get overridden`() {
        try {
            mode.execute(when (scriptType()) {
                SupportedScriptTypes.PY -> "def print(): pass"

                // Javascript doesn't seem to have a way to protect against this...
                SupportedScriptTypes.JS -> return
            })
        } catch (e: ScriptExecutionException) {
            // an error in the attempt is also acceptable
            return
        }

        mode.execute(fnCall("print", "magic"))
        assert(judo.prints)
            .containsExactly("magic")
    }

    @Test fun `Second readFile() call clears old declarations from same file`() {
        when (scriptType()) {
            SupportedScriptTypes.PY -> mode.readLikeFile("test.py", """
                alias('shiny $1', 'Mighty fine $1')

                @event('CONNECTED')
                def on_connect(): pass

                nmap('gkf', 'ikeep flyin<cr>')
                prompt('foo $1>', '[$1]')

                @trigger('foo')
                def on_foo(): pass
            """.trimIndent())

            SupportedScriptTypes.JS -> mode.readLikeFile("test.js", """
                alias('shiny $1', 'Mighty fine $1');

                event('CONNECTED', function onConnect() {});

                nmap('gkf', 'ikeep flyin<cr>');
                prompt('foo $1>', '[$1]');

                trigger('foo', function onFoo() {});
            """.trimIndent())
        }

        assert(judo.aliases.hasAliasFor("shiny $1")).isTrue()
        assert(judo.events.has("CONNECTED")).isTrue()
        assert(judo.events.has("DISCONNECTED")).isFalse()
        assert(judo.prompts.size).isEqualTo(1)
        assert(judo.maps).contains(listOf("normal", "gkf", "ikeep flyin<cr>", true))
        assert(judo.triggers.hasTriggerFor("foo")).isTrue()

        // read a different file, and no change to original
        when (scriptType()) {
            SupportedScriptTypes.PY -> mode.readLikeFile("different.py", """
                @event("DISCONNECTED")
                def on_disconnect(): pass
            """.trimIndent())

            SupportedScriptTypes.JS -> mode.readLikeFile("different.js", """
                event("DISCONNECTED", function onDisconnect() {});
            """.trimIndent())
        }
        assert(judo.aliases.hasAliasFor("shiny $1")).isTrue()
        assert(judo.events.has("CONNECTED")).isTrue()
        assert(judo.events.has("DISCONNECTED")).isTrue()
        assert(judo.prompts.size).isEqualTo(1)
        assert(judo.maps).contains(listOf("normal", "gkf", "ikeep flyin<cr>", true))
        assert(judo.triggers.hasTriggerFor("foo")).isTrue()

        // read the original file (which is now inexplicably empty) and
        // clear out the things it created...
        val ext = scriptType().name.toLowerCase()
        mode.readLikeFile("test.$ext","")
        assert(judo.aliases.hasAliasFor("shiny $1")).isFalse()
        assert(judo.events.has("CONNECTED")).isFalse()
        assert(judo.maps).isEmpty()
        assert(judo.prompts.size).isEqualTo(0)
        assert(judo.triggers.hasTriggerFor("foo")).isFalse()

        // ... but the other file is intact
        assert(judo.events.has("DISCONNECTED")).isTrue()
    }

    @Test fun `Render help items in columns`() {
        val colWidth = "createUserMode  ".length
        renderer.settableWindowWidth = colWidth * 2

        mode.showHelp()

        assert(judo.prints)
            .startsWith("alias           cmap            ")
    }

    @Test fun `Render help for vars`() {
        mode.showHelp("judo")
        assert(judo.prints)
            .startsWith(
                "judo",
                "===="
            )
    }

    @Test fun `Support connect() with host and port`() {
        mode.execute("""
            connect("host", 23)
        """.trimIndent())

        assert(judo.connects).containsExactly(
            createURI("host:23")
        )
    }

    @Test fun `Support connect() with schema-less URI`() {
        mode.execute("""
            connect("host:port")
        """.trimIndent())

        assert(judo.connects).containsExactly(
            createURI("host:port")
        )
    }

    @Test fun `Support connect() with SSL schema URI`() {
        mode.execute("""
            connect("ssl://host:port")
        """.trimIndent())

        assert(judo.connects).containsExactly(
            createURI("ssl://host:port")
        )
    }

    @Test(timeout = 10_000) fun `Support interrupt`() = runBlocking {
        // warm up the engine
        mode.execute("")

        GlobalScope.launch {
            delay(50)
            mode.interrupt()
        }

        try {
            when (scriptType()) {
                SupportedScriptTypes.PY -> mode.execute("""
                    loops = 1
                    while True:
                        loops += 1
                """.trimIndent())

                SupportedScriptTypes.JS -> mode.execute("""
                    var loops = 1
                    while (true) {
                        ++loops;
                    }
                """.trimIndent())
            }
        } catch (e: ScriptExecutionException) {
            // normal; carry on
        }

        // ensure scripting isn't broken
        mode.execute(fnCall("print", "mreynolds"))
        assert(judo.prints).containsExactly("mreynolds")
    }
}

private fun <T> Assert<List<T>>.startsWith(vararg sequence: T) {
    val actualFirstSequence = actual.take(sequence.size)
    if (actualFirstSequence == sequence.asList()) return
    expected("to start with ${show(sequence)}, but was ${show(actualFirstSequence)}")
}

private fun CmdMode.readLikeFile(fileName: String, fileContents: String) =
    readFile(File(fileName), ByteArrayInputStream(fileContents.toByteArray()))
