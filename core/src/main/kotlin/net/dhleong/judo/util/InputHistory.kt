package net.dhleong.judo.util

import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import java.io.File

class InputHistory(val buffer: InputBuffer, capacity: Int = 2000): IInputHistory {

    private val contents = CircularArrayList<String>(capacity)

    private var lastBufferValue: String? = null
    private var historyOffset = 0

    val size: Int
        get() = contents.size

    override fun clear() {
        contents.clear()
        lastBufferValue = null
        historyOffset = 0
    }

    override fun push(line: String) {
        if (contents.isEmpty() || contents.last() != line) {
            contents.add(line)
        }
    }

    override fun resetHistoryOffset() {
        historyOffset = 0
    }

    /**
     * Scroll the history by [dir], where positive numbers onMove to more recent
     * items and negative numbers onMove to older, updating the attached InputBuffer
     */
    override fun scroll(dir: Int, clampCursor: Boolean) {
        if (lastBufferValue == null && dir < 0) {
            lastBufferValue = buffer.toString()
        }

        historyOffset = maxOf(
            -contents.size,
            minOf(0, historyOffset + dir)
        )

        if (historyOffset == 0) {
            lastBufferValue?.let {
                buffer.set(it, clampCursor)
                lastBufferValue = null
            }
        } else {
            buffer.set(contents[contents.size + historyOffset], clampCursor)
        }
    }

    /**
     * Search backwards from the current historyOffset position
     *  for a string matching the [match]. If a match was found,
     *  the buffer is set to that value; if not, nothing will change
     *
     * @return True if a match was found, else false.
     */
    override fun search(match: String, forceNext: Boolean): Boolean {
        val matches = predicateForSearch(match)
        // NOTE offset will always be negative (or 0)
        val offset =
            if (forceNext && contents[contents.lastIndex + historyOffset].matches()) {
                historyOffset - 1
            } else {
                historyOffset
            }

        val original = buffer.toChars() // avoid string object creation
        for (i in contents.size + offset - 1 downTo 0) {
            if (contents[i].matches()
                    && !(forceNext && original.isEqualTo(contents[i]))) {
                // TODO if contents[i] IS equal to original, we could prune it?
                buffer.set(contents[i])
                historyOffset = -(contents.size - i - 1)
                return true
            }
        }

        return false
    }

    private fun predicateForSearch(match: String): String.() -> Boolean {
        if (' ' !in match) {
            // simple search
            return { contains(match, ignoreCase = true) }
        }

        // per-word match
        val words = match.split(" ")
        return {
            // every "word" has to be found AFTER the previous, AND
            // after a word break (IE: whitespace)
            var last = 0
            words.all {
                if (last == -1) {
                    // there was no more word boundary after the last word
                    false
                } else {
                    val wordIndex = indexOf(it, last, ignoreCase = true)
                    if (wordIndex == -1) {
                        // no dice
                        false
                    } else {
                        // found! keep going (after the next whitespace)
                        last = indexOf(' ', wordIndex)
                        true
                    }
                }
            }
        }
    }

    fun writeTo(path: File) {
        if (!path.exists()) {
            if (!path.parentFile.isDirectory && !path.parentFile.mkdirs()) {
                throw IllegalArgumentException("Couldn't create directories for $path")
            }
        }

        path.bufferedWriter().use { writer ->
            contents.forEach { writer.appendln(it) }
        }
    }
}

internal fun CharSequence.isEqualTo(other: String): Boolean {
    if (length != other.length) return false

    // suppress because as-is, this doesn't create any
    // allocations; the super cool call chain version does
    @Suppress("LoopToCallChain")
    for (i in indices) {
        if (this[i] != other[i]) return false
    }

    return true
}
