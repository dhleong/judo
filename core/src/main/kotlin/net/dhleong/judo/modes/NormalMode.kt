package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.InputBufferProvider
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.charMotion
import net.dhleong.judo.motions.findMotion
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.motions.wordMotion
import net.dhleong.judo.motions.xCharMotion
import net.dhleong.judo.util.InputHistory
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

class NormalMode(
        judo: IJudoCore,
        buffer: InputBuffer,
        val history: InputHistory
) : BaseModeWithBuffer(judo, buffer),
    MappableMode,
    InputBufferProvider {

    override val userMappings = KeyMapping()
    override val name = "normal"

    private val mapping = KeyMapping(
        keys(":") to { core -> core.enterMode("cmd") },

        keys("a") to { core ->
            applyMotion(charMotion(1))
            core.enterMode("insert")
        },
        keys("A") to { core ->
            applyMotion(toEndMotion())
            core.enterMode("insert")
        },

        keys("c", "c") to { core ->
            buffer.clear()
            core.enterMode("insert")
        },

        keys("d", "d") to { _ ->
            buffer.clear()
        },

        keys("f") to motionAction(findMotion(1)),
        keys("F") to motionAction(findMotion(-1)),

        keys("G") to { core -> core.scrollToBottom() },

        keys("i") to { core -> core.enterMode("insert") },
        keys("I") to { core ->
            applyMotion(toStartMotion())
            core.enterMode("insert")
        },

        // browse history
        keys("j") to { _ -> history.scroll(1) },
        keys("k") to { _ -> history.scroll(-1) },

        keys("x") to actionOn(xCharMotion(1)) { _, range ->
            buffer.delete(range)
        },
        keys("X") to actionOn(xCharMotion(-1)) { _, range ->
            buffer.delete(range)
            buffer.cursor = range.endInclusive
        },

        // TODO counts?
        keys("b") to motionAction(wordMotion(-1, false)),
        keys("B") to motionAction(wordMotion(-1, true)),
        keys("w") to motionAction(wordMotion(1, false)),
        keys("W") to motionAction(wordMotion(1, true)),

        // TODO counts?
        keys("h") to motionAction(charMotion(-1)),
        keys("l") to motionAction(charMotion(1)),

        keys("0") to motionAction(toStartMotion()),
        keys("$") to motionAction(toEndMotion()),

        keys("ctrl b") to { core -> core.scrollPages(1) },
        keys("ctrl f") to { core -> core.scrollPages(-1) },
        keys("ctrl c") to { _ -> clearBuffer() },
        keys("ctrl r") to { core -> core.enterMode("rsearch") }
    )

    private val input = MutableKeys()

    override fun onEnter() {
        input.clear()
        buffer.cursor = maxOf(0, buffer.cursor - 1)
    }

    override fun feedKey(key: KeyStroke, remap: Boolean) {
        if (key.keyCode == KeyEvent.VK_ENTER) {
            judo.send(buffer.toString(), false)
            clearBuffer()
            return
        }

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

    override fun renderInputBuffer(): String = buffer.toString()
    override fun getCursor(): Int = buffer.cursor

    private fun clearBuffer() {
        input.clear()
        buffer.clear()
    }
}


