package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.OperatorFunc
import net.dhleong.judo.StateKind
import net.dhleong.judo.input.CountReadingBuffer
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.motions.ALL_MOTIONS
import net.dhleong.judo.motions.Motion
import net.dhleong.judo.motions.normalizeForMotion
import net.dhleong.judo.motions.repeat
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
    private val count = CountReadingBuffer()

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

        if (count.tryPush(key)) {
            return
        }

        if (tryMappings(key, remap, input, mapping, null)) {
            return
        }

        judo.exitMode()
    }

    private fun opFuncActionWith(motion: Motion): KeyAction =
        { _ ->
            val range = repeat(motion, count.toRepeatCount())
                .calculate(judo, buffer)
                .normalizeForMotion(motion)
//            val range = motion.calculate(judo, buffer)
//                .normalizeForMotion(motion)

            judo.exitMode()
            opfunc(range)

            count.clear()
        }

}
