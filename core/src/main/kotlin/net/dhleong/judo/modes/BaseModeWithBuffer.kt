package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.Mode
import net.dhleong.judo.complete.CompletionSuggester
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.motions.Motion
import net.dhleong.judo.motions.normalizeForMotion

/**
 * @author dhleong
 */

typealias KeyActionOnRange = (IJudoCore, IntRange) -> Unit

abstract class BaseModeWithBuffer(
    val judo: IJudoCore,
    val buffer: InputBuffer
) : Mode {

    protected fun actionOn(motion: Motion, action: KeyActionOnRange): KeyAction =
        { judo ->
            val range = rangeOf(motion)
            if (range.endInclusive >= 0) {
                action(judo, range)
            }
        }

    protected fun applyMotion(motion: Motion, clampCursor: Boolean = true) {
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

    protected fun rangeOf(motion: Motion) =
        motion.calculate(judo, buffer)
            .normalizeForMotion(motion)

    /** @return True if we handled it as a mapping (or might yet) */
    protected fun tryMappings(
        key: Key, allowRemap: Boolean,
        input: MutableKeys,
        originalMaps: KeyMapping, remaps: KeyMapping?
    ): Boolean {

        input.push(key)

        if (allowRemap && remaps != null) {
            remaps.match(input)?.let {
                input.clear()
                it.invoke(judo)
                return true
            }

            if (remaps.couldMatch(input)) {
                return true
            }
        }

        originalMaps.match(input)?.let {
            input.clear()
            it.invoke(judo)
            return true
        }

        if (originalMaps.couldMatch(input)) {
            return true
        }

        input.clear() // no possible matches; clear input queue
        return false
    }

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
