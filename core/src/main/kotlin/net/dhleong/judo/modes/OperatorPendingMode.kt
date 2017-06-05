package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.OperatorFunc
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.motions.ALL_MOTIONS
import net.dhleong.judo.motions.Motion
import javax.swing.KeyStroke

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
        val currentOpFunc = judo.opfunc
            ?: throw IllegalStateException("Entered `op` without setting opfunc")

        opfunc = currentOpFunc
        judo.opfunc = null

        currentFullLineMotionKey = fullLineMotionKey
        fullLineMotionKey = 0.toChar()

        input.clear()
    }

    override fun feedKey(key: KeyStroke, remap: Boolean) {

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
            val range = motion.calculate(judo, buffer)
            opfunc(range)
            judo.exitMode()
        }

}