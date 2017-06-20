package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
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
        sendHistory.resetHistoryOffset()
    }

    override fun feedKey(key: KeyStroke, remap: Boolean, fromMap: Boolean) {
        when {
            key.keyCode == KeyEvent.VK_ENTER -> {
                judo.send(buffer.toString(), fromMap)
                judo.exitMode()
                clearBuffer()
                return
            }

            key.keyChar == 'r' -> {
                if (key.hasCtrl()) {
                    trySearch(true)
                    return
                }
            }

            key.keyChar == 'c' -> {
                if (key.hasCtrl()) {
                    clearBuffer()
                    judo.exitMode()
                    return
                }
            }
        }

        if (key.hasCtrl()) {
            // ignore
            return
        }

        val oldLength = searchBuffer.size
        searchBuffer.type(key)

        if (searchBuffer.size < oldLength) {
            // we deleted stuff; this invalidates the search progress
            sendHistory.resetHistoryOffset()
        }

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

    override fun getCursor(): Int =
        SEARCH_BUFFER_PREFIX.length + searchBuffer.cursor

    private fun clearBuffer() {
        buffer.clear()
        searchBuffer.clear()
        sendHistory.resetHistoryOffset()
    }

}