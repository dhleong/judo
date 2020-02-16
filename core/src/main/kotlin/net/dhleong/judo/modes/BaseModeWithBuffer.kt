package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.Mode
import net.dhleong.judo.complete.CompletionSuggester
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.motions.Motion
import net.dhleong.judo.motions.normalizeForMotion

/**
 * @author dhleong
 */

typealias KeyActionOnRange = suspend (IJudoCore, IntRange) -> Unit

abstract class BaseModeWithBuffer(
    val judo: IJudoCore,
    val buffer: InputBuffer
) : Mode {

    open fun clearBuffer() {
        buffer.clear()
    }

    protected fun actionOn(motion: Motion, action: KeyActionOnRange): KeyAction =
        { judo ->
            val range = rangeOf(motion)
            if (range.last >= 0) {
                action(judo, range)
            }
        }

    protected suspend fun applyMotion(motion: Motion, clampCursor: Boolean = true) {
        motion.applyTo(judo, buffer)
        if (clampCursor) {
            clampCursor(buffer)
        }
    }

    protected open fun clampCursor(buffer: InputBuffer) {
         if (buffer.cursor > buffer.lastIndex) {
             buffer.cursor = maxOf(0, buffer.lastIndex)
         }
    }

    /**
     * Convenience to create a KeyAction that just applies
     *  the given motion
     */
    protected fun motionAction(motion: Motion): KeyAction =
        { applyMotion(motion) }

    protected suspend fun rangeOf(motion: Motion) =
        motion.calculate(judo, buffer)
            .normalizeForMotion(motion)

    protected fun performTabCompletionFrom(key: Key, suggester: CompletionSuggester) {
        if (key.hasShift()) {
            rewindTabCompletion(suggester)
        } else {
            performTabCompletion(suggester)
        }
    }

    private fun performTabCompletion(suggester: CompletionSuggester) {
        if (!suggester.isInitialized()) {
            suggester.initialize(buffer.toChars(), buffer.cursor)
        }

        suggester.updateWithNextSuggestion(buffer)
    }

    private fun rewindTabCompletion(suggester: CompletionSuggester) {
        if (!suggester.isInitialized()) return // nop

        suggester.updateWithPrevSuggestion(buffer)
    }
}
