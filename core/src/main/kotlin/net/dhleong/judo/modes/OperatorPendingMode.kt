package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.OperatorFunc
import net.dhleong.judo.StateKind
import net.dhleong.judo.input.CountReadingBuffer
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.action
import net.dhleong.judo.motions.ALL_MOTIONS
import net.dhleong.judo.motions.Motion
import net.dhleong.judo.motions.normalizeForMotion
import net.dhleong.judo.motions.repeat

val KEY_OPFUNC = StateKind<OperatorFunc>("net.dhleong.judo.modes.op.opfunc")
val KEY_LAST_OP = StateKind<Motion>("net.dhleong.judo.modes.op.lastOp")

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

    override suspend fun feedKey(key: Key, remap: Boolean, fromMap: Boolean) {

        // special case for eg dd, cc, etc
        if (key.char == currentFullLineMotionKey) {
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

    private fun opFuncActionWith(motion: Motion): KeyAction = action {
        val lastOp = repeat(motion, count.toRepeatCount())
        judo.state[KEY_LAST_OP] = lastOp

        val range = lastOp
            .calculate(judo, buffer)
            .normalizeForMotion(motion)

        judo.exitMode()
        opfunc(range)

        count.clear()
    }

}
