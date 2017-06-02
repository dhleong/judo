package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.Motion
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.motions.wordMotion
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
            buffer.moveCursor(1)
            core.enterMode("insert")
        },
        keys("A") to { core ->
            toEndMotion().applyTo(buffer)
            core.enterMode("insert")
        },

        keys("c", "c") to { core ->
            buffer.clear()
            core.enterMode("insert")
        },

        keys("d", "d") to { _ ->
            buffer.clear()
        },

        keys("G") to { core -> core.scrollToBottom() },

        keys("i") to { core -> core.enterMode("insert") },
        keys("I") to { core ->
            toStartMotion().applyTo(buffer)
            core.enterMode("insert")
        },

        // TODO counts?
        keys("b") to motionAction(wordMotion(-1, false)),
        keys("B") to motionAction(wordMotion(-1, true)),
        keys("w") to motionAction(wordMotion(1, false)),
        keys("W") to motionAction(wordMotion(1, true)),

        keys("0") to motionAction(toStartMotion()),
        keys("$") to motionAction(toEndMotion()),

        keys("ctrl b") to { core -> core.scrollPages(1) },
        keys("ctrl f") to { core -> core.scrollPages(-1) },
        keys("ctrl c") to { _ -> clearBuffer() }
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

    /**
     * Convenience to create a KeyAction that just applies
     *  the given motion
     */
    private fun motionAction(motion: Motion): KeyAction =
        { _ -> motion.applyTo(buffer) }
}


