package net.dhleong.judo.trigger

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.trigger.multi.RangeMultiTrigger
import net.dhleong.judo.util.PatternSpec

interface MultiTrigger {
    val id: String
    val options: MultiTriggerOptions
    fun process(line: FlavorableCharSequence): MultiTriggerResult
}

/**
 * @author dhleong
 */
class MultiTriggerManager : IMultiTriggerManager {

    private val triggers = mutableListOf<MultiTrigger>()

    override fun define(
        id: String,
        type: MultiTriggerType,
        options: MultiTriggerOptions,
        patterns: List<PatternSpec>,
        processor: MultiTriggerProcessor
    ) {
        undefine(id)

        triggers += when (type) {
            MultiTriggerType.RANGE -> RangeMultiTrigger(
                id,
                options,
                patterns[0],
                patterns[1],
                processor
            )
        }
    }

    override fun process(input: FlavorableCharSequence): MultiTriggerResult {
        for (trigger in triggers) {
            val result = trigger.process(input)
            if (result !is MultiTriggerResult.Ignore) {
                return result
            }
        }

        return MultiTriggerResult.Ignore
    }

    override fun undefine(id: String) {
        triggers.removeIf { it.id == id }
    }

    override fun clear() {
        triggers.clear()
    }

    override fun clear(entry: String) = undefine(entry)

    operator fun get(id: String) = triggers.first { it.id == id }
}

fun IMultiTriggerManager.processMultiTriggers(
    buffer: IJudoBuffer,
    judo: IJudoCore,
    input: FlavorableCharSequence
): Boolean {
    when (val result = process(input)) {
        is MultiTriggerResult.Restore -> {
            buffer.deleteLast()
            for (l in result.lines) {
                buffer.appendLine(l)
            }
            buffer.appendLine(input)
            judo.printRaw("ERROR: processing multi-trigger ${result.triggerId}: ${result.reason}")

            // as for Process, these lines need to be logged
            judo.logging.log(result.lines)
            return false // "unhandled"; allow other processing
        }

        is MultiTriggerResult.Error -> {
            judo.printRaw("ERROR: processing multi-trigger ${result.triggerId}: ${result.reason}")
            return false
        }

        is MultiTriggerResult.Delete -> {
            buffer.deleteLast()
            return true // stop processing
        }

        is MultiTriggerResult.Process -> {
            if (result.result is MultiTriggerResult.Delete) {
                buffer.deleteLast()
            }
            result.process()

            // NOTE: since we consumed these lines, the logger has not
            // seen them yet; pass them now
            judo.logging.log(result.flavoredLines)
            return true
        }

        is MultiTriggerResult.Consume -> return true
        is MultiTriggerResult.Ignore -> return false
    }
}
