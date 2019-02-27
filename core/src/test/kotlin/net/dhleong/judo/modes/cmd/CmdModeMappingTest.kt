package net.dhleong.judo.modes.cmd

import assertk.assert
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import net.dhleong.judo.input.Key
import net.dhleong.judo.script.ScriptingEngine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeMappingTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

    @Test fun `nnoremap creates maps`() {
        mode.execute(fnCall("nnoremap", "a", "bc"))

        assert(judo.maps)
            .containsExactly(listOf("normal", "a", "bc", false))
    }

    @Test fun `nnoremap supports functions`() {
        val fnDef = when (scriptType()) {
            SupportedScriptTypes.JS -> """
                function action() {
                    print("mapped!");
                }
            """.trimIndent()

            SupportedScriptTypes.PY -> """
                def action(): print("mapped!")
            """.trimIndent()
        }
        mode.execute(fnDef)
        mode.execute(fnCall("nnoremap", "a", Var("action")))

        judo.feedKey(Key.ofChar('a'))

        assert(judo.prints).containsExactly("mapped!")
    }

    @Test fun `createMap creates custom mode maps`() {
        mode.execute(fnCall("createMap", "custom", "a", "bc", true))

        assert(judo.maps)
            .containsExactly(listOf("custom", "a", "bc", true))
    }

    @Test fun `unmap removes mappings`() {
        mode.execute(fnCall("map", "a", "b"))
        mode.execute(fnCall("unmap", "a"))

        assert(judo.maps).isEmpty()
    }

    @Test fun `deleteMap removes mappings`() {
        mode.execute(fnCall("createMap", "custom", "a", "bc", true))
        mode.execute(fnCall("deleteMap", "custom", "a"))

        assert(judo.maps).isEmpty()
    }

}
