package net.dhleong.judo.modes.cmd

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.support.expected
import assertk.assertions.support.show
import kotlinx.coroutines.runBlocking
import net.dhleong.judo.hasLines
import net.dhleong.judo.hasSize
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.trigger.MultiTriggerManager
import net.dhleong.judo.trigger.MultiTriggerOptions
import net.dhleong.judo.trigger.processMultiTriggers
import org.junit.Ignore
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
    @Ignore("TODO")
    @Test fun `multitrigger detects runaway trigger`() = runBlocking {
    }

    @Test fun `multitrigger accepts an options map`() = runBlocking {
        registerMultiTrigger(
            id = "deleteFlags",
            optionsMap = "{'maxLines': 1}",
            flags = "delete"
        )
        assertThat(judo.multiTriggers["deleteFlags"].options).all {
            hasMaxLines(1)
            isNotColor()
            isDelete()
        }
    }

    @Test fun `multitrigger options map merges with flags`() = runBlocking {
        registerMultiTrigger(
            id = "deleteFlags",
            optionsMap = "{'maxLines': 1, 'color': true}",
            flags = "delete"
        )
        assertThat(judo.multiTriggers["deleteFlags"].options).all {
            hasMaxLines(1)
            isColor()
            isDelete()
        }

        registerMultiTrigger(
            id = "colorFlags",
            optionsMap = "{'maxLines': 1, 'delete': true}",
            flags = "color"
        )
        assertThat(judo.multiTriggers["deleteFlags"].options).all {
            hasMaxLines(1)
            isColor()
            isDelete()
        }
    }

    @Test fun `multitrigger handles options map with NO flags`() = runBlocking {
        registerMultiTrigger(
            id = "deleteFlags",
            optionsMap = "{'maxLines': 1, 'delete': true}",
            flags = null
        )
        assertThat(judo.multiTriggers["deleteFlags"].options).all {
            hasMaxLines(1)
            isNotColor()
            isDelete()
        }
    }

    private suspend fun registerMultiTrigger(
        id: String,
        optionsMap: String,
        flags: String?
    ) {
        val flagsString = flags?.let { ", '$it'" } ?: ""
        mode.execute(when (scriptType()) {
            SupportedScriptTypes.PY -> """
                import re
                @multitrigger('$id', 'range', ${optionsMap.replace("true", "True")}, [
                   "Take my love",
                   "Take me where",
                ]$flagsString)
                def handleTrigger(lines): pass
            """.trimIndent()

            SupportedScriptTypes.JS -> """
                multitrigger('$id', 'range', $optionsMap, [
                   "Take my love",
                   "Take me where",
                ]$flagsString, function(lines) {});
            """.trimIndent()
        })
    }

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

private fun Assert<MultiTriggerOptions>.hasMaxLines(lines: Int) = given { actual ->
    if (actual.maxLines == lines) return
    expected("maxLines == ${show(lines)} but was ${show(actual.maxLines)}")
}

private fun Assert<MultiTriggerOptions>.isColor() = given { actual ->
    if (actual.color) return
    expected("color but did not have color")
}

private fun Assert<MultiTriggerOptions>.isNotColor() = given { actual ->
    if (!actual.color) return
    expected("NOT color but have color")
}

private fun Assert<MultiTriggerOptions>.isDelete() = given { actual ->
    if (actual.delete) return
    expected("delete but did not have delete")
}
