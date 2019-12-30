package net.dhleong.judo.modes.cmd

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import kotlinx.coroutines.runBlocking
import net.dhleong.judo.hasLines
import net.dhleong.judo.hasSize
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.trigger.MultiTriggerManager
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author dhleong
 */
@RunWith(Parameterized::class)
class CmdModeMultiTriggerTest(
    factory: ScriptingEngine.Factory
) : AbstractCmdModeTest(factory) {

    @Test fun `RangeMultiTrigger extracts all lines`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                import re
                @multitrigger('id', 'range', [
                   re.compile(r'^Take my love'),
                   re.compile(r'^Take me where'),
                ], 'delete')
                def handleTrigger(lines): 
                    for l in lines:
                        print(l)
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                multitrigger('id', 'range', [
                   "Take my love",
                   "Take me where",
                ], 'delete', function(lines) {
                    for (i=0; i < lines.length; ++i) {
                        print(lines[i]);
                    }
                });
            """.trimIndent()
        })

        val buffer = judo.renderer.currentTabpage.currentWindow.currentBuffer
        judo.multiTriggers.apply {
            process("Take my love")
            process("Take my land")

            // multi trigger is consuming
            assertThat(buffer).hasSize(0)
            assertThat(judo.prints).isEmpty()

            process("Take me where")
        }
        assertThat(judo.prints).hasSize(3)

        @Suppress("UNCHECKED_CAST")
        assertThat(judo.prints).containsExactly(
            "Take my love\n",
            "Take my land\n",
            "Take me where\n"
        )
    }

    @Test fun `RangeMultiTrigger arg can be passed to Buffer#set`() = runBlocking {
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                import re
                @multitrigger('id', 'range', [
                   re.compile(r'^Take my love'),
                   re.compile(r'^Take me where'),
                ], 'delete')
                def handleTrigger(lines): 
                    judo.current.buffer.set(lines)
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                multitrigger('id', 'range', [
                   "Take my love",
                   "Take me where",
                ], 'delete', function(lines) {
                    judo.current.buffer.set(lines)
                });
            """.trimIndent()
        })

        val buffer = judo.renderer.currentTabpage.currentWindow.currentBuffer
        judo.multiTriggers.apply {
            process("Take my love")
            process("Take my land")

            // multi trigger is consuming
            assertThat(buffer).hasSize(0)
            assertThat(judo.prints).isEmpty()

            process("Take me where")
        }
        assertThat(buffer).hasSize(3)

        @Suppress("UNCHECKED_CAST")
        assertThat(buffer).hasLines(
            "Take my love\n",
            "Take my land\n",
            "Take me where\n"
        )
    }
}

fun MultiTriggerManager.process(string: String) = process(FlavorableStringBuilder.withDefaultFlavor(
    string + "\n"
))
