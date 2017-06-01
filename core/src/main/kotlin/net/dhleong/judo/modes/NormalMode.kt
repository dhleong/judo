package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

class NormalMode(val judo: IJudoCore, val buffer: InputBuffer) : MappableMode {
    override val userMappings = KeyMapping()
    override val name = "normal"

    private val mapping = KeyMapping(
        keys(":") to { core -> core.enterMode("cmd") },

        keys("a") to { core ->
            buffer.cursor = minOf(buffer.size, buffer.cursor + 1)
            core.enterMode("insert")
        },
        keys("A") to { core ->
            buffer.cursor = buffer.size
            core.enterMode("insert")
        },

        keys("c", "c") to { core ->
            buffer.clear()
            core.enterMode("insert")
        },

        keys("d", "d") to { core ->
            buffer.clear()
        },

        keys("i") to { core -> core.enterMode("insert") },
        keys("I") to { core ->
            buffer.cursor = 0
            core.enterMode("insert")
        },

        keys("ctrl C") to { core -> clearBuffer() }
    )

    private val input = MutableKeys()

    override fun onEnter() {
        input.clear()
    }

    override fun feedKey(key: KeyStroke, remap: Boolean) {
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

        input.clear()
    }

    private fun clearBuffer() {
        input.clear()
        buffer.clear()
    }

}
