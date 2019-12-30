package net.dhleong.judo.trigger.multi

import net.dhleong.judo.net.toAnsi
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.trigger.MultiTrigger
import net.dhleong.judo.trigger.MultiTriggerOptions
import net.dhleong.judo.trigger.MultiTriggerProcessor
import net.dhleong.judo.trigger.MultiTriggerResult
import net.dhleong.judo.util.PatternSpec

/**
 * @author dhleong
 */
class RangeMultiTrigger(
    override val id: String,
    private val options: MultiTriggerOptions,
    private val start: PatternSpec,
    private val stop: PatternSpec,
    private val processor: MultiTriggerProcessor
) : MultiTrigger {

    private var reading = false
    private val buffer = JudoBuffer(id = -1, scrollbackSize = options.maxLines)

    override fun process(line: FlavorableCharSequence): MultiTriggerResult =
        if (reading) processEnd(line)
        else processStart(line)

    private fun processStart(line: FlavorableCharSequence): MultiTriggerResult {
        if (!start.matcher(line).find()) return MultiTriggerResult.Ignore

        reading = true
        buffer.append(line)
        if (options.delete) {
            return MultiTriggerResult.Delete
        }

        return MultiTriggerResult.Consume
    }

    private fun processEnd(line: FlavorableCharSequence): MultiTriggerResult {
        if (stop.matcher(line).find()) {
            reading = false
            buffer.append(line)

            val lines = buffer.consumeStringLines()

            this.processor(lines)

            return if (options.delete) MultiTriggerResult.Delete
                else MultiTriggerResult.Consume
        }

        val giveUp = buffer.size >= options.maxLines
        return when {
            giveUp && options.delete -> MultiTriggerResult.Restore(
                id,
                buffer.consumeLines()
            )

            giveUp -> {
                buffer.clear()
                MultiTriggerResult.Consume
            }

            else -> {
                buffer.append(line)
                if (options.delete) MultiTriggerResult.Consume
                else MultiTriggerResult.Delete
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
