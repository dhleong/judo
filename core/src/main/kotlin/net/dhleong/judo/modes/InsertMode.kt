package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.InputBufferProvider
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.Motion
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
class InsertMode(val judo: IJudoCore, val buffer: InputBuffer) : MappableMode, InputBufferProvider {

    override val userMappings = KeyMapping()
    override val name = "insert"

    private val mapping = KeyMapping(
        // not strictly vim, but nice enough
        keys("ctrl a") to motionAction(toStartMotion()),
        keys("ctrl e") to motionAction(toEndMotion()),

        keys("ctrl b") to { core -> core.scrollPages(1) },
        keys("ctrl f") to { core -> core.scrollPages(-1) },

        keys("ctrl r") to { core -> core.enterMode("rsearch") }
    )
    private val input = MutableKeys()

    override fun onEnter() {
        // nop
    }

    override fun feedKey(key: KeyStroke, remap: Boolean) {
        when (key.keyCode) {
            KeyEvent.VK_ENTER -> {
                judo.send(buffer.toString(), false)
                clearBuffer()
                return
            }
        }

        // TODO share this code with NormalMode?
        input.push(key)

        if (remap) {
            userMappings.match(input)?.let {
                input.clear()
                it.invoke(judo)
                return
            }

            if (userMappings.couldMatch(input)) {
                return
            }
        }

        mapping.match(input)?.let {
            input.clear()
            it.invoke(judo)
            return
        }

        if (mapping.couldMatch(input)) {
            return
        }

        // no possible mapping; just update buffer
        buffer.type(key)
        input.clear() // and clear input queue
    }

    override fun renderInputBuffer(): String = buffer.toString()
    override fun getCursor(): Int = buffer.cursor

    private fun clearBuffer() {
        input.clear()
        buffer.clear()
    }

    /**
     * Convenience to create a KeyAction that just applies
     *  the given motion
     */
    private fun motionAction(motion: Motion): KeyAction =
        { _ -> motion.applyTo(buffer) }
}
