package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.OperatorFunc
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.Motion
import net.dhleong.judo.motions.charMotion
import net.dhleong.judo.motions.findMotion
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.motions.wordMotion
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

        // TODO counts?
        keys("b") to opFuncActionWith(wordMotion(-1, false)),
        keys("B") to opFuncActionWith(wordMotion(-1, true)),
        keys("w") to opFuncActionWith(wordMotion(1, false)),
        keys("W") to opFuncActionWith(wordMotion(1, true)),

        keys("f") to opFuncActionWith(findMotion(1)),
        keys("F") to opFuncActionWith(findMotion(-1)),

        // TODO counts?
        keys("h") to opFuncActionWith(charMotion(-1)),
        keys("l") to opFuncActionWith(charMotion(1)),

        keys("0") to opFuncActionWith(toStartMotion()),
        keys("$") to opFuncActionWith(toEndMotion())
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
