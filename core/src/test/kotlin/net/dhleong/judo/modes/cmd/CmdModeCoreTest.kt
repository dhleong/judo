package net.dhleong.judo.modes.cmd

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.message
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.modes.USER_CONFIG_FILE
import net.dhleong.judo.script.ScriptingEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeCoreTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

    @Test fun echo() {
        mode.execute("echo('test', 2)")

        assert(judo.echos).containsExactly("test", 2)
    }

    @Test fun globals() {
        mode.execute("echo(MYJUDORC)")

        assert(judo.echos).containsExactly(USER_CONFIG_FILE.absolutePath)
    }

    @Test fun `var definitions are shared across execute() calls`() {

        mode.execute("""
            value = "magic"
            """.trimIndent())

        mode.execute("""
            echo(value)
            """.trimIndent())

        assert(judo.echos).containsExactly("magic")
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

    @Test fun `config('setting') echoes its value`() {
        assert(WORD_WRAP !in judo.state).isTrue()

        mode.execute(fnCall("config", "wordwrap"))
        assert(judo.echos).containsExactly("wordwrap = true (default)")
        judo.echos.clear()

        mode.execute(fnCall("config", "wordwrap", false))
        assert(judo.echos).containsExactly("wordwrap = false")
    }

    @Test fun `Don't allow builtins to get overridden`() {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> "def echo(): pass"

            // Javascript doesn't seem to have a way to protect against this...
            SupportedScriptTypes.JS -> return
        })

        mode.execute(fnCall("echo", "magic"))
        assert(judo.echos)
            .containsExactly("magic")
    }

}