package net.dhleong.judo.util

import net.dhleong.judo.input.InputBuffer

class InputHistory(val buffer: InputBuffer, capacity: Int = 2000) {

    // TODO circular buffer
    val contents = ArrayList<String>(capacity)

    var lastBufferValue: String? = null
    var historyOffset = 0

    fun clear() {
        contents.clear()
        lastBufferValue = null
        historyOffset = 0
    }

    fun push(line: String) {
        contents.add(line)
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
}