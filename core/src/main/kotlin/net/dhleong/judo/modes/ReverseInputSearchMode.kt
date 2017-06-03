package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.InputBufferProvider
import net.dhleong.judo.Mode
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.util.InputHistory
import net.dhleong.judo.util.hasCtrl
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class ReverseInputSearchMode(
    val judo: IJudoCore,
    val buffer: InputBuffer,
    val sendHistory: InputHistory
) : Mode, InputBufferProvider {

    override val name = "rsearch"

    private val SEARCH_BUFFER_PREFIX = "(reverse-i-search)`"
    private val SEARCH_BUFFER_SUFFIX = "': "

    val searchBuffer = InputBuffer()

    override fun onEnter() {
        searchBuffer.clear()
    }

    override fun feedKey(key: KeyStroke, remap: Boolean) {
        when (key.keyCode) {
            KeyEvent.VK_ENTER -> {
                judo.send(buffer.toString(), false)
                judo.exitMode()
                clearBuffer()
                return
            }

            KeyEvent.VK_R -> {
                if (key.hasCtrl()) {
                    trySearch(true)
                    return
                }
            }
        }

        searchBuffer.type(key)
        trySearch(false)
    }

    private fun trySearch(forceNext: Boolean) {
        val found = sendHistory.search(searchBuffer.toString(), forceNext)
        if (!found) {
            // TODO bell?
        }
    }

    override fun renderInputBuffer(): String =
        "$SEARCH_BUFFER_PREFIX$searchBuffer$SEARCH_BUFFER_SUFFIX$buffer"

    // TODO actually the cursor should be at the match location
    override fun getCursor(): Int =
        SEARCH_BUFFER_PREFIX.length + searchBuffer.cursor

    private fun clearBuffer() {
        buffer.clear()
        searchBuffer.clear()
    }

}