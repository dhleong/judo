package net.dhleong.judo.trigger

import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.util.Clearable
import net.dhleong.judo.util.PatternSpec

enum class MultiTriggerType {
    /**
     * Captures a range of text using a single "start"
     * and a single "end" pattern
     */
    RANGE
}

data class MultiTriggerOptions(
    val color: Boolean = false,
    val delete: Boolean = false,
    val maxLines: Int = 100
)

sealed class MultiTriggerResult {
    /** Ignore the processed line, IE: add it to the buffer */
    object Ignore : MultiTriggerResult()

    /**
     * Consume the processed line, IE: don't pass it to other
     * MultiTriggers, but otherwise Ignore it
     */
    object Consume : MultiTriggerResult()

    /** Delete the processed line */
    object Delete : MultiTriggerResult()

    /**
     * Restore all of the provided [lines] into the buffer and
     * ignore the processed line (IE: append it *after* the
     * restored lines).
     *
     * This is an error result.
     */
    class Restore(
        val triggerId: String,
        val lines: List<FlavorableCharSequence>
    ) : MultiTriggerResult()

    /**
     * Stop processing due to an error with a multi trigger
     * with the given [triggerId]
     */
    class Error(
        val triggerId: String
    ) : MultiTriggerResult()
}

/**
 * @author dhleong
 */
interface IMultiTriggerManager : Clearable<String> {
    fun define(
        id: String,
        type: MultiTriggerType,
        options: MultiTriggerOptions,
        patterns: List<PatternSpec>,
        processor: TriggerProcessor
    )

    fun process(input: FlavorableCharSequence): MultiTriggerResult

    fun undefine(id: String)
}