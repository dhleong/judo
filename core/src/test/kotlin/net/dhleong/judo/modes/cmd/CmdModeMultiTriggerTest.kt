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
import net.dhleong.judo.trigger.processMultiTriggers
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
                   "Take my love",
                   "I cannot stand",
                ], 'delete')
                def handleTrigger(lines): 
                    for l in lines:
                        echo(l)
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                multitrigger('id', 'range', [
                   "Take my love",
                   "I cannot stand",
                ], 'delete', function(lines) {
                    for (i=0; i < lines.length; ++i) {
                        echo(lines[i]);
                    }
                });
            """.trimIndent()
        })

        val buffer = judo.renderer.currentTabpage.currentWindow.currentBuffer
        judo.multiTriggers.apply {
            process("Take my love")
            process("Take my land")
            process("Take me where")

            // multi trigger is deleting the lines
            assertThat(buffer).hasSize(0)
            assertThat(judo.prints).isEmpty()

            process("I cannot stand")
        }
        assertThat(buffer).hasSize(0)
        assertThat(judo.echos).hasSize(4)

        @Suppress("UNCHECKED_CAST")
        assertThat(judo.echos).containsExactly(
            "Take my love\n",
            "Take my land\n",
            "Take me where\n",
            "I cannot stand\n"
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

        assertThat(buffer).hasLines(
            "Take my love\n",
            "Take my land\n",
            "Take me where\n"
        )
    }

    fun MultiTriggerManager.process(string: String) {
        // it might be nice to have an integration test with JudoCore.onIncomingBuffer,
        // but this is close enough:
        val buffer = judo.renderer.currentTabpage.currentWindow.currentBuffer
        val line = FlavorableStringBuilder.withDefaultFlavor(string + "\n")
        buffer.append(line)

        processMultiTriggers(buffer, judo, line)
    }
}
