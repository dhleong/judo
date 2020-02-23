package net.dhleong.judo.modes

import net.dhleong.judo.CursorType
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.OperatorFunc
import net.dhleong.judo.StateKind
import net.dhleong.judo.input.CountReadingBuffer
import net.dhleong.judo.input.IBufferWithCursor
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapHelper
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.action
import net.dhleong.judo.motions.ALL_MOTIONS
import net.dhleong.judo.motions.LINEWISE_MOTIONS
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
    buffer: IBufferWithCursor,
    private val includeLinewiseMotions: Boolean = false
) : BaseModeWithBuffer(judo, buffer) {

    override val name = "op"

    var fullLineMotionKey: Char = 0.toChar()

    private lateinit var opfunc: OperatorFunc
    private var currentFullLineMotionKey: Char = 0.toChar()

    private val mapping = KeyMapping(
        gatherMotions().map { (keys, motion) ->
            keys to opFuncActionWith(motion)
        }
    )

    private fun gatherMotions() =
        ALL_MOTIONS + (
            if (includeLinewiseMotions) LINEWISE_MOTIONS
            else emptyList()
        )

    private val keymaps = KeyMapHelper(judo, mapping)

    private val count = CountReadingBuffer()

    override fun onEnter() {
        val currentOpFunc = judo.state[KEY_OPFUNC]
            ?: throw IllegalStateException("Entered `op` without setting opfunc")

        opfunc = currentOpFunc
        judo.state.remove(KEY_OPFUNC)

        currentFullLineMotionKey = fullLineMotionKey
        fullLineMotionKey = 0.toChar()

        keymaps.clearInput()
        judo.setCursorType(CursorType.UNDERSCORE_BLINK)
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

        if (keymaps.tryMappings(key, remap)) {
            return
        }

        judo.exitMode()
    }

    private fun opFuncActionWith(motion: Motion): KeyAction = action {
        val lastOp = repeat(motion, count.toRepeatCount())
        judo.state[KEY_LAST_OP] = lastOp

        val range = lastOp
            .calculateLinewise(judo, buffer)
            .normalizeForMotion(motion)

        judo.exitMode()
        opfunc(range)

        count.clear()
    }

}

fun withOperator(
    judo: IJudoCore,
    count: CountReadingBuffer,
    buffer: IBufferWithCursor,
    action: OperatorFunc,
    enterOperatorPendingMode: IJudoCore.() -> Unit = { enterMode("op") }
) {
    // save now before we clear when leaving normal mode
    val repeats = count.toRepeatCount()

    judo.state[KEY_OPFUNC] = { originalRange ->

        action(originalRange)

        if (repeats > 1) {
            judo.state[KEY_LAST_OP]?.let { lastOp ->
                val range = repeat(lastOp.toRepeatable(), repeats - 1)
                    .calculateLinewise(judo, buffer)
                    .normalizeForMotion(lastOp)

                action(range)
            }
        }
    }

    judo.enterOperatorPendingMode()
}
