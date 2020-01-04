package net.dhleong.judo.trigger.multi

import net.dhleong.judo.EmptyStateMap
import net.dhleong.judo.net.toAnsi
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.trigger.MultiTrigger
import net.dhleong.judo.trigger.MultiTriggerOptions
import net.dhleong.judo.trigger.MultiTriggerProcessor
import net.dhleong.judo.trigger.MultiTriggerResult
import net.dhleong.judo.util.PatternSpec

private val MultiTriggerOptions.consumeResult: MultiTriggerResult
    get() = if (delete) MultiTriggerResult.Delete
        else MultiTriggerResult.Consume

/**
 * @author dhleong
 */
class RangeMultiTrigger(
    override val id: String,
    override val options: MultiTriggerOptions,
    private val start: PatternSpec,
    private val stop: PatternSpec,
    private val processor: MultiTriggerProcessor
) : MultiTrigger {

    private var reading = false
    private val buffer = JudoBuffer(
        id = -1,
        settings = EmptyStateMap,
        scrollbackSize = options.maxLines
    )

    override fun process(line: FlavorableCharSequence): MultiTriggerResult =
        if (reading) processEnd(line)
        else processStart(line)

    private fun processStart(line: FlavorableCharSequence): MultiTriggerResult {
        if (!start.matcher(line).find()) return MultiTriggerResult.Ignore

        reading = true
        buffer.append(line)
        return options.consumeResult
    }

    private fun processEnd(line: FlavorableCharSequence): MultiTriggerResult {
        if (stop.matcher(line).find()) {
            reading = false
            buffer.append(line)

            val lines = buffer.consumeStringLines()

            return MultiTriggerResult.Process(
                processor,
                lines,
                options.consumeResult
            )
        }

        val giveUp = buffer.size >= options.maxLines
        return when {
            giveUp && options.delete -> MultiTriggerResult.Restore(
                id,
                "end not found after ${options.maxLines} lines",
                buffer.consumeLines()
            )

            giveUp -> {
                buffer.clear()
                MultiTriggerResult.Error(
                    id,
                    "end not found after ${options.maxLines} lines"
                )
            }

            else -> {
                buffer.append(line)
                options.consumeResult
            }
        }
    }

    private fun IJudoBuffer.consumeStringLines(): List<String> =
        List(size) {
            if (options.color) this[it].toAnsi()
            else this[it].toString()
        }.also { clear() }

    private fun IJudoBuffer.consumeLines(): List<FlavorableCharSequence> =
        List(size) { this[it] }
            .also { clear() }
}
