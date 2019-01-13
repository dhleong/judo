package net.dhleong.judo.modes.cmd

import assertk.assert
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
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
        assert(judo.prompts.size).isEqualTo(1)
        var lastPrompt: String? = null
        val result = judo.prompts.process("Input(42)".parseAnsi()) { _, prompt ->
            lastPrompt = prompt
        }
        assert(result).isEmpty()
        assert(lastPrompt).isEqualTo("prompt 42>")
    }

    @Test fun `prompt() handles 'color' flag`() {
        mode.execute(fnCall(
            "prompt",
            "^Input($1)", "color", "prompt $1>"
        ))

        assert(judo.prints).isEmpty()
        assert(judo.prompts.size).isEqualTo(1)
        var lastPrompt: String? = null
        val result = judo.prompts.process("Input(${ansi(1,2)}42)".parseAnsi()) { _, prompt ->
            lastPrompt = prompt
        }
        assert(result).isEmpty()
        assert(lastPrompt).isEqualTo("prompt ${ansi(1,2)}42${ansi(0)}>")
    }
}
