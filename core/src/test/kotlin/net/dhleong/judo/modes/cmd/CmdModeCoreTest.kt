package net.dhleong.judo.modes.cmd

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
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

    @Test fun print() = runBlocking {
        mode.execute("print('test', 2)")

        assertThat(judo.prints).containsExactly("test", 2)
    }

    @Test fun globals() = runBlocking {
        mode.execute("print(MYJUDORC)")

        assertThat(judo.prints).containsExactly(mode.userConfigFile.absolutePath)
    }

    @Test fun `var definitions are shared across execute() calls`() = runBlocking {
        mode.execute("""
            value = "magic"
        """.trimIndent())

        mode.execute("""
            print(value)
        """.trimIndent())

        assertThat(judo.prints).containsExactly("magic")
    }

    @Test fun `Complain with unexpected number of args`() = runBlocking {
        assertThat {
            mode.execute(fnCall("send", "mreynolds"))
            assertThat(judo.sends).containsExactly("mreynolds")
        }.isSuccess()

        assertThat {
            mode.execute(fnCall("send"))
        }.isFailure().all {
            message().isNotNull().all {
                contains("arguments")
            }
        }

        assertThat {
            mode.execute(fnCall("send", "niska", 2))
        }.isFailure().all {
            message().isNotNull().all {
                contains("arguments")
            }
        }
    }

    @Test fun settings() = runBlocking {
        assertThat(WORD_WRAP !in judo.state).isTrue()

        assertThat {
            mode.execute(fnCall("config", "nonsense", true))
        }.isFailure().all {
            message().isNotNull().all {
                contains("No such setting")
            }
        }

        assertThat {
            mode.execute(fnCall("config", "wordwrap", "string?"))
        }.isFailure().all {
            message().isNotNull().all {
                contains("is invalid for")
            }
        }

        mode.execute(fnCall("config", "wordwrap", true))

        assertThat(judo.state[WORD_WRAP]).isEqualTo(true)
    }

    @Test fun `config('setting') prints its value`() = runBlocking {
        assertThat(WORD_WRAP !in judo.state).isTrue()

        mode.execute(fnCall("config", "wordwrap"))
        assertThat(judo.prints).containsExactly("wordwrap = true (default)")
        judo.prints.clear()

        mode.execute(fnCall("config", "wordwrap", false))
        assertThat(judo.prints).containsExactly("wordwrap = false")
    }

    @Test fun `Don't allow builtins to get overridden`() = runBlocking {
        try {
            mode.execute(when (scriptType()) {
                SupportedScriptTypes.PY -> "def print(): pass"

                // Javascript doesn't seem to have a way to protect against this...
                SupportedScriptTypes.JS -> return@runBlocking
            })
        } catch (e: ScriptExecutionException) {
            // an error in the attempt is also acceptable
            return@runBlocking
        }

        mode.execute(fnCall("print", "magic"))
        assertThat(judo.prints)
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

        assertThat(judo.aliases.hasAliasFor("shiny $1")).isTrue()
        assertThat(judo.events.has("CONNECTED")).isTrue()
        assertThat(judo.events.has("DISCONNECTED")).isFalse()
        assertThat(judo.prompts.size).isEqualTo(1)
        assertThat(judo.maps).contains(listOf("normal", "gkf", "ikeep flyin<cr>", true))
        assertThat(judo.triggers.hasTriggerFor("foo")).isTrue()

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
        assertThat(judo.aliases.hasAliasFor("shiny $1")).isTrue()
        assertThat(judo.events.has("CONNECTED")).isTrue()
        assertThat(judo.events.has("DISCONNECTED")).isTrue()
        assertThat(judo.prompts.size).isEqualTo(1)
        assertThat(judo.maps).contains(listOf("normal", "gkf", "ikeep flyin<cr>", true))
        assertThat(judo.triggers.hasTriggerFor("foo")).isTrue()

        // read the original file (which is now inexplicably empty) and
        // clear out the things it created...
        val ext = scriptType().name.toLowerCase()
        mode.readLikeFile("test.$ext","")
        assertThat(judo.aliases.hasAliasFor("shiny $1")).isFalse()
        assertThat(judo.events.has("CONNECTED")).isFalse()
        assertThat(judo.maps).isEmpty()
        assertThat(judo.prompts.size).isEqualTo(0)
        assertThat(judo.triggers.hasTriggerFor("foo")).isFalse()

        // ... but the other file is intact
        assertThat(judo.events.has("DISCONNECTED")).isTrue()
    }

    @Test fun `Render help items in columns`() {
        val colWidth = "createUserMode  ".length
        renderer.settableWindowWidth = colWidth * 2

        mode.showHelp()

        assertThat(judo.prints)
            .startsWith("alias           cmap            ")
    }

    @Test fun `Render help for vars`() {
        mode.showHelp("judo")
        assertThat(judo.prints)
            .startsWith(
                "judo",
                "===="
            )
    }

    @Test fun `Support connect() with host and port`() = runBlocking {
        mode.execute("""
            connect("host", 23)
        """.trimIndent())

        assertThat(judo.connects).containsExactly(
            createURI("host:23")
        )
    }

    @Test fun `Support connect() with schema-less URI`() = runBlocking {
        mode.execute("""
            connect("host:port")
        """.trimIndent())

        assertThat(judo.connects).containsExactly(
            createURI("host:port")
        )
    }

    @Test fun `Support connect() with SSL schema URI`() = runBlocking {
        mode.execute("""
            connect("ssl://host:port")
        """.trimIndent())

        assertThat(judo.connects).containsExactly(
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
        assertThat(judo.prints).containsExactly("mreynolds")
    }
}

private fun <T> Assert<List<T>>.startsWith(vararg sequence: T) = given { actual ->
    val actualFirstSequence = actual.take(sequence.size)
    if (actualFirstSequence == sequence.asList()) return
    expected("to start with ${show(sequence)}, but was ${show(actualFirstSequence)}")
}

private fun CmdMode.readLikeFile(fileName: String, fileContents: String) =
    readFile(File(fileName), ByteArrayInputStream(fileContents.toByteArray()))
