package net.dhleong.judo.input

import java.awt.event.KeyEvent.VK_BACK_SPACE
import javax.swing.KeyStroke

/**
 * A navigable input buffer
 *
 * @author dhleong
 */
class InputBuffer {
    var cursor = 0

    private val buffer = StringBuilder(128)

    fun clear() {
        buffer.setLength(0)
        cursor = 0
    }

    fun get(index: Int): Char = buffer[index]

    fun isEmpty() = buffer.isEmpty()

    /**
     * Type the given KeyStroke into the buffer at the current cursor
     */
    fun type(key: KeyStroke) {
        when (key.keyCode) {
            VK_BACK_SPACE -> {
                if (cursor > 0 && cursor == buffer.length) {
                    --cursor
                    buffer.setLength(cursor)
                } else if (cursor > 0) {
                    // complicated mess
                    val after = buffer.subSequence(cursor, buffer.length)
                    buffer.setLength(--cursor)
                    buffer.append(after)
                }
                return
            }
        }

        if (cursor == buffer.length) {
            buffer.append(key.keyChar)
        } else {
            buffer.insert(cursor, key.keyChar)
        }

        ++cursor
    }

    fun toChars(): CharSequence = buffer

    override fun toString() = buffer.toString()

    val size: Int
        get() = buffer.length

    fun set(value: String) {
        buffer.setLength(0)
        buffer.append(value)
        cursor = value.length
    }

    /*
     Cursor movement
     */

    fun moveCursor(distance: Int) {
        cursor = maxOf(
            0,
            minOf(size, cursor + distance)
        )
    }

    fun replace(range: IntRange, replacement: CharSequence) {
        buffer.replace(range.start, range.endInclusive + 1, replacement.toString())
    }
}
