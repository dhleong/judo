package net.dhleong.judo.modes.cmd

import assertk.Assert
import assertk.assert
import assertk.assertAll
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.message
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.prompt.IPromptManager
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.parseAnsi
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.util.ansi
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModePromptTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

    @Test fun prompt() {
        mode.execute(fnCall("prompt", "^Input($1)", "prompt $1>"))

        assert(judo.prints).isEmpty()
        assert(judo.prompts).hasSize(1)
        assert(judo.prompts).processes("Input(42)".parseAnsi(), into = "prompt 42>")
    }

    @Test fun `prompt() handles 'color' flag`() {
        mode.execute(fnCall(
            "prompt",
            "^Input($1)", "color", "prompt $1>"
        ))

        assert(judo.prints).isEmpty()
        assert(judo.prompts).hasSize(1)
        assert(judo.prompts).processes(
            "Input(${ansi(1,2)}42)".parseAnsi(),
            into = "prompt ${ansi(1,2)}42${ansi(0)}>"
        )
    }

    @Test fun `prompt() rejects illegal group id`() {
        assert {
            mode.execute(fnCall(
                "prompt",
                -2, "^Input($1)", "prompt $1>"
            ))
        }.thrownError {
            message().isNotNull {
                it.contains("group must be > 0")
            }
        }
    }

    @Test fun `prompt() handles groups`() {
        mode.execute(fnCall(
            "prompt",
            22, "^Input($1)", "prompt $1>"
        ))

        assert(judo.prints).isEmpty()
        assert(judo.prompts).hasSize(1)
        assert(judo.prompts).processes("Input(42)".parseAnsi(), into = "prompt 42>")
    }

    @Test fun `prompt() handles group with flags`() {
        mode.execute(fnCall(
            "prompt",
            2, "^Input($1)", "color", "prompt $1>"
        ))

        assert(judo.prints).isEmpty()
        assert(judo.prompts).hasSize(1)
        assert(judo.prompts).processes(
            "Input(${ansi(1,2)}42)".parseAnsi(),
            into = "prompt ${ansi(1,2)}42${ansi(0)}>"
        )
    }

    @Test fun `prompt() as function decorator`() {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@prompt('^hp: $1')
                |def handlePrompt(hp): return "awesome %s" % hp
            """.trimMargin()

            // Most languages, sadly, don't support decorators:
            SupportedScriptTypes.JS -> return
        })

        assert(judo.prompts).hasSize(1)
        assert(judo.prompts).processes(
            "hp: 42".parseAnsi(),
            into = "awesome 42"
        )
    }

    @Test fun `prompt() with group as function decorator`() {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@prompt(42, '^hp: $1')
                |def handlePrompt(hp): return "awesome %s" % hp
            """.trimMargin()

            // Most languages, sadly, don't support decorators:
            SupportedScriptTypes.JS -> return
        })

        assert(judo.prompts).hasSize(1)
        assert(judo.prompts).processes(
            "hp: 42".parseAnsi(),
            into = "awesome 42"
        )
    }

    @Test fun `prompt() with group and flags as function decorator`() {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                |@prompt(42, '^hp: $1', 'color')
                |def handlePrompt(hp): return "awesome %s" % hp
            """.trimMargin()

            // Most languages, sadly, don't support decorators:
            SupportedScriptTypes.JS -> return
        })

        assert(judo.prompts).hasSize(1)
        assert(judo.prompts).processes(
            "hp: ${ansi(1,2)}42".parseAnsi(),
            into = "awesome ${ansi(1,2)}42${ansi(0)}"
        )
    }

}

private fun Assert<IPromptManager>.hasSize(size: Int) {
    if (actual.size == size) return
    expected("size = ${show(size)} but was ${show(actual.size)}")
}
private fun Assert<IPromptManager>.processes(line: FlavorableCharSequence, into: String) {
    var lastPrompt: String? = null
    val result = actual.process(line) { _, prompt, _ ->
        lastPrompt = prompt
    }
    assertAll {
        assert(result, "result").isEmpty()
        assert(lastPrompt, "processed prompt").isEqualTo(into)
    }
}
