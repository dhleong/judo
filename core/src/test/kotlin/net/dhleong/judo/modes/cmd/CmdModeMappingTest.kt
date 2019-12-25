package net.dhleong.judo.modes.cmd

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlinx.coroutines.runBlocking
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

    @Test fun `nnoremap creates maps`() = runBlocking {
        mode.execute(fnCall("nnoremap", "a", "bc"))

        assertThat(judo.maps)
            .containsExactly(listOf("normal", "a", "bc", false))
    }

    @Test fun `nnoremap supports functions`() = runBlocking {
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

        assertThat(judo.prints).containsExactly("mapped!")
    }

    @Test fun `createMap creates custom mode maps`() = runBlocking {
        mode.execute(fnCall("createMap", "custom", "a", "bc", true))

        assertThat(judo.maps)
            .containsExactly(listOf("custom", "a", "bc", true))
    }

    @Test fun `unmap removes mappings`() = runBlocking {
        mode.execute(fnCall("map", "a", "b"))
        mode.execute(fnCall("unmap", "a"))

        assertThat(judo.maps).isEmpty()
    }

    @Test fun `deleteMap removes mappings`() = runBlocking {
        mode.execute(fnCall("createMap", "custom", "a", "bc", true))
        mode.execute(fnCall("deleteMap", "custom", "a"))

        assertThat(judo.maps).isEmpty()
    }

}
