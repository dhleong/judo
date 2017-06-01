package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.Mode
import net.dhleong.judo.input.InputBuffer
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.InputStream
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

abstract class BaseCmdMode(val judo: IJudoCore) : Mode {

    override val name = "cmd"

    val inputBuffer = InputBuffer()

    override fun feedKey(key: KeyStroke, remap: Boolean) {
        if (key.modifiers != 0 && key.modifiers != InputEvent.SHIFT_DOWN_MASK) {
            // TODO mappings?
            return
        }
        if (key.keyCode == KeyEvent.VK_ENTER) {
            val code = inputBuffer.toString()
            when (code) {
                "q" -> {
                    judo.quit()
                    return
                }
            }
            execute(code)
            clearInputBuffer()
            exitMode()
        }

        insertChar(key)
    }

    private fun exitMode() {
        judo.echo("exit cmd")
        judo.enterMode("normal") // TODO return to previous mode?
    }

    /**
     * Insert a key stroke at the current cursor position
     */
    private fun insertChar(key: KeyStroke) {
        val wasEmpty = inputBuffer.isEmpty()
        inputBuffer.type(key)
        if (inputBuffer.isEmpty() && wasEmpty) {
            exitMode()
        }
    }

    override fun onEnter() {
        clearInputBuffer()
    }

    abstract fun execute(code: String)

    abstract fun readFile(fileName: String, stream: InputStream)

    private fun clearInputBuffer() {
        inputBuffer.clear()
    }
}
