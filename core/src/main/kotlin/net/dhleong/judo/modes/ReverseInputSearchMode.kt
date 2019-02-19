package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.Mode
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.render.toFlavorable

class ReverseInputSearchMode(
    val judo: IJudoCore,
    val buffer: InputBuffer,
    private val sendHistory: IInputHistory
) : Mode, InputBufferProvider {

    override val name = "rsearch"

    private val SEARCH_BUFFER_PREFIX = "(reverse-i-search)`"
    private val SEARCH_BUFFER_SUFFIX = "': "

    val searchBuffer = InputBuffer()

    override fun onEnter() {
        searchBuffer.clear()
        sendHistory.resetHistoryOffset()
    }

    override fun feedKey(key: Key, remap: Boolean, fromMap: Boolean) {
        when {
            key == Key.ENTER -> {
                judo.submit(buffer.toString(), fromMap)
                judo.exitMode()
                clearBuffer()
                return
            }

            key.char == 'r' -> {
                if (key.hasCtrl()) {
                    trySearch(true)
                    return
                }
            }

            key.char == 'c' -> {
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

    override fun renderInputBuffer() =
        "$SEARCH_BUFFER_PREFIX$searchBuffer$SEARCH_BUFFER_SUFFIX$buffer".toFlavorable()

    override fun getCursor(): Int =
        SEARCH_BUFFER_PREFIX.length + searchBuffer.cursor

    private fun clearBuffer() {
        buffer.clear()
        searchBuffer.clear()
        sendHistory.resetHistoryOffset()
    }

}