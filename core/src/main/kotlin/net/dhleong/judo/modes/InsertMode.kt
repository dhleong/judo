package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
class InsertMode(val judo: IJudoCore, val buffer: InputBuffer) : MappableMode {

    override val userMappings = KeyMapping()
    override val name = "insert"

    private val mapping = KeyMapping()
    private val input = MutableKeys()

    override fun onEnter() {
        // nop
    }

    override fun feedKey(key: KeyStroke, remap: Boolean) {
        when (key.keyCode) {
            KeyEvent.VK_ENTER -> {
                judo.send(buffer.toString())
                clearBuffer()
                return
            }
        }

        if (mapping.couldMatch(input)) {
            input.push(key)
            mapping.match(input)?.let {
                it.invoke(judo)
                input.clear()
            }
            return
        } else {
            input.clear()
        }

        // no possible mapping; just update buffer
        buffer.type(key)
    }

    private fun clearBuffer() {
        input.clear()
        buffer.clear()
    }
}
