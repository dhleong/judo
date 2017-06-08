package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.Mode
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.util.hasCtrl
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
        when {
            key.keyCode == KeyEvent.VK_ENTER -> {
                val code = inputBuffer.toString()
                when (code) {
                    "q", "q!", "qa", "qa!" -> {
                        judo.quit()
                        return
                    }
                }
                execute(code)
                clearBuffer()
                exitMode()
                return
            }

            key.keyChar == 'a' && key.hasCtrl() -> {
                inputBuffer.cursor = 0
                return
            }

            key.keyChar == 'c' && key.hasCtrl() -> {
                clearBuffer()
                exitMode()
                return
            }

            key.keyChar == 'e' && key.hasCtrl() -> {
                inputBuffer.cursor = inputBuffer.size
                return
            }

        }

        if (key.hasCtrl()) {
            // ignore
            return
        }

        insertChar(key)
    }

    private fun exitMode() {
        judo.exitMode()
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
        clearBuffer()
    }

    abstract fun execute(code: String)

    abstract fun readFile(fileName: String, stream: InputStream)

    private fun clearBuffer() {
        inputBuffer.clear()
    }
}
