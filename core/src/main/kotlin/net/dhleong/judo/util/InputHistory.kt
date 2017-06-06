package net.dhleong.judo.util

import net.dhleong.judo.input.InputBuffer

class InputHistory(val buffer: InputBuffer, capacity: Int = 2000) {

    // TODO circular buffer
    private val contents = ArrayList<String>(capacity)

    private var lastBufferValue: String? = null
    private var historyOffset = 0

    fun clear() {
        contents.clear()
        lastBufferValue = null
        historyOffset = 0
    }

    fun push(line: String) {
        contents.add(line)
    }

    fun resetHistoryOffset() {
        historyOffset = 0
    }

    /**
     * Scroll the history by [dir], where positive numbers move to more recent
     * items and negative numbers move to older, updating the attached InputBuffer
     */
    fun scroll(dir: Int) {
        if (lastBufferValue == null && dir < 0) {
            lastBufferValue = buffer.toString()
        }

        historyOffset = maxOf(
            -contents.size,
            minOf(0, historyOffset + dir)
        )

        if (historyOffset == 0) {
            lastBufferValue?.let {
                buffer.set(it)
                lastBufferValue = null
            }
        } else {
            buffer.set(contents[contents.size + historyOffset])
        }
    }

    /**
     * Search backwards from the current historyOffset position
     *  for a string matching the [match]. If a match was found,
     *  the buffer is set to that value; if not, nothing will change
     *
     * @return True if a match was found, else false.
     */
    fun search(match: String, forceNext: Boolean): Boolean {
        // NOTE offset will always be negative (or 0)
        val offset =
            if (forceNext &&
                    contents[contents.lastIndex + historyOffset].contains(match, true)) {
                historyOffset - 1
            } else {
                historyOffset
            }

        for (i in contents.size + offset - 1 downTo 0) {
            if (contents[i].contains(match, true)) {
                buffer.set(contents[i])
                historyOffset = -(contents.size - i - 1)
                return true
            }
        }

        return false
    }
}