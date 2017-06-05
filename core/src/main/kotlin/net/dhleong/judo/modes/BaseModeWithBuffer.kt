package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.Mode
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.motions.Motion

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

    protected fun applyMotion(motion: Motion) =
        motion.applyTo(judo::readKey, buffer)

    /**
     * Convenience to create a KeyAction that just applies
     *  the given motion
     */
    protected fun motionAction(motion: Motion): KeyAction =
        { _ -> applyMotion(motion) }

    protected fun rangeOf(motion: Motion) =
        motion.calculate(judo, buffer)
}
