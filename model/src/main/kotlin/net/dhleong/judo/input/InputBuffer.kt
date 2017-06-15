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

    val lastIndex: Int
        get() = buffer.lastIndex

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

    fun delete(range: IntRange) {
        buffer.delete(range.start, range.endInclusive + 1)
    }

    fun deleteWithCursor(range: IntRange, clampCursor: Boolean = true): Boolean {
        normalizeRange(range)?.let { (normalized, newCursor) ->
            delete(normalized)
            if (clampCursor) {
                cursor = minOf(buffer.lastIndex, newCursor)
            } else {
                // don't clamp *within* buffer, but also don't allow going completely
                // outside of it. that is the path to errors
                cursor = minOf(buffer.length, newCursor)
            }
            return true
        }

        return false
    }

    fun replace(range: IntRange, replacement: CharSequence) {
        buffer.replace(range.start, range.endInclusive + 1, replacement.toString())
    }

    fun replace(range: IntRange, transformer: (CharSequence) -> CharSequence) {
        val old = buffer.subSequence(range.start, range.endInclusive + 1)
        val new = transformer(old).toString()
        buffer.replace(range.start, range.endInclusive + 1, new)
    }

    fun replaceWithCursor(range: IntRange, transformer: (CharSequence) -> CharSequence) {
        normalizeRange(range)?.let { (normalized, newCursor) ->
            replace(normalized, transformer)
            cursor = newCursor
        }
    }

    fun switchCaseWithCursor(range: IntRange) {
        replaceWithCursor(range) { old ->
            val result = StringBuilder(old.length)
            old.forEach {
                if (Character.isUpperCase(it)) {
                    result.append(Character.toLowerCase(it))
                } else {
                    result.append(Character.toUpperCase(it))
                }
            }
            result
        }
    }

    private fun normalizeRange(range: IntRange): Pair<IntRange, Int>? {
        if (range.start < range.endInclusive) {
            // forward delete
            val end = (range.endInclusive - 1)
            if (end < 0) return null

            return (range.start..end) to minOf(buffer.lastIndex, range.start)
        } else {
            val end = (range.start - 1)
            if (end < 0) return null

            return (range.endInclusive..end) to minOf(buffer.lastIndex, range.endInclusive)
        }
    }
}
