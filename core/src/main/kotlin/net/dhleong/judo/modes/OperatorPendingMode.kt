package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.OperatorFunc
import net.dhleong.judo.StateKind
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.motions.ALL_MOTIONS
import net.dhleong.judo.motions.Motion
import javax.swing.KeyStroke

val KEY_OPFUNC = StateKind<OperatorFunc>("net.dhleong.judo.modes.op.opfunc")

/**
 * @author dhleong
 */
class OperatorPendingMode(
    judo: IJudoCore,
    buffer: InputBuffer
) : BaseModeWithBuffer(judo, buffer) {

    override val name = "op"

    var fullLineMotionKey: Char = 0.toChar()

    private lateinit var opfunc: OperatorFunc
    private var currentFullLineMotionKey: Char = 0.toChar()

    // FIXME refactor so we only have to enumerate motions once
    //  and get them in both normal mode movements and here
    private val mapping = KeyMapping(
        ALL_MOTIONS.map { (keys, motion) ->
            keys to opFuncActionWith(motion)
        }
    )

    private val input = MutableKeys()

    override fun onEnter() {
        val currentOpFunc = judo.state[KEY_OPFUNC]
            ?: throw IllegalStateException("Entered `op` without setting opfunc")

        opfunc = currentOpFunc
        judo.state.remove(KEY_OPFUNC)

        currentFullLineMotionKey = fullLineMotionKey
        fullLineMotionKey = 0.toChar()

        input.clear()
    }

    override fun feedKey(key: KeyStroke, remap: Boolean, fromMap: Boolean) {

        // special case for eg dd, cc, etc
        if (key.keyChar == currentFullLineMotionKey) {
            judo.exitMode()
            opfunc(buffer.size..0)
            return
        }

        input.push(key)

        mapping.match(input)?.let {
            input.clear()
            it.invoke(judo)
            return
        }

        if (mapping.couldMatch(input)) {
            return
        }

        judo.exitMode()
    }

    private fun opFuncActionWith(motion: Motion): KeyAction =
        { _ ->
            var range = motion.calculate(judo, buffer)

            // TODO can we put this anywhere better?
            if (motion.isInclusive && range.start < range.endInclusive) {
                range = range.start..(range.endInclusive + 1)
            } else if (motion.isInclusive && range.start > range.endInclusive) {
                range = range.start..(range.endInclusive + 1)
            }

            judo.exitMode()
            opfunc(range)
        }

}
