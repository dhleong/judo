package net.dhleong.judo.input

import net.dhleong.judo.input.changes.AmendableUndoable
import net.dhleong.judo.input.changes.UndoManager
import net.dhleong.judo.register.IRegisterManager
import kotlin.properties.Delegates

/**
 * A navigable input buffer
 *
 * @author dhleong
 */
class InputBuffer(
    private val registers: IRegisterManager? = null,
    val undoMan: UndoManager = UndoManager()
) : IBufferWithCursor, TextDocument {
    override var cursor: Int by Delegates.observable(0) { _, _, newValue ->
        if (newValue < 0 || newValue > size) {
            throw IllegalArgumentException(
                "Illegal cursor position: $newValue (size=$size)")
        }
    }

    private val buffer = StringBuilder(128)

    fun beginChangeSet() = undoMan.beginChangeSet(this)
    inline fun inChangeSet(block: () -> Unit) = undoMan.inChangeSet(this, block)

    override fun clear() {
        buffer.setLength(0)
        cursor = 0
    }

    override fun get(charOffset: Int): Char = buffer[charOffset]
    override fun get(startOffset: Int, endOffset: Int): CharSequence = get(startOffset..endOffset)

    override fun get(range: IntRange): CharSequence =
        normalizeRange(range)?.let { buffer.substring(it.first) }
            ?: ""

    override fun isEmpty() = buffer.isEmpty()

    override val lastIndex: Int
        get() = buffer.lastIndex

    override val length: Int
        get() = buffer.length

    /**
     * Type the given Key into the buffer at the current cursor
     */
    fun type(key: Key) {
        when (key.keyCode) {
            Key.CODE_BACKSPACE -> {
                if (cursor > 0 && cursor == buffer.length) {
                    amendBackspaceUndo(cursor - 1, buffer[cursor - 1])

                    --cursor
                    buffer.setLength(cursor)
                } else if (cursor > 0) {
                    amendBackspaceUndo(cursor - 1, buffer[cursor - 1])

                    // complicated mess
                    val after = buffer.subSequence(cursor, buffer.length)
                    buffer.setLength(--cursor)
                    buffer.append(after)
                }
                return
            }
        }

        if (cursor == buffer.length) {
            buffer.append(key.char)
        } else {
            buffer.insert(cursor, key.char)
        }
        amendInsertUndo(cursor, 1)

        ++cursor
    }

    override fun toChars(): CharSequence = buffer

    override fun toString() = buffer.toString()

    override val size: Int
        get() = buffer.length

    /**
     * @param clampCursor If True (default), will prevent the cursor from going past
     *  buffer.size-1; if false, it will still clamp it to buffer.size
     */
    fun set(value: String, clampCursor: Boolean = true) {
        buffer.setLength(0)
        buffer.append(value)

        cursor = if (clampCursor) {
            maxOf(0, value.length - 1)
        } else {
            value.length
        }
    }

    fun delete(range: IntRange) {
        registers?.let {
            it.current.copyFrom(
                buffer,
                range.first,
                minOf(buffer.length, range.last + 1)
            )

            inChangeSet {
                val insertValue = it.current.value.toString()
                undoMan.current.addUndoAction { input ->
                    input.buffer.insert(range.first, insertValue)
                    input.cursor = range.first
                }
            }

            it.resetCurrent()
        }

        buffer.delete(range.first, range.last + 1)
    }

    fun deleteWithCursor(range: IntRange, clampCursor: Boolean = true): Boolean {
        normalizeRange(range)?.let { (normalized, newCursor) ->
            delete(normalized)
            cursor = if (clampCursor) {
                // NOTE: lastIndex is -1 when size == 0
                maxOf(0, minOf(buffer.lastIndex, newCursor))
            } else {
                // don't clamp *within* buffer, but also don't allow going completely
                // outside of it. that is the path to errors
                minOf(buffer.length, newCursor)
            }
            return true
        }

        return false
    }

    fun insert(index: Int, value: CharSequence) {
        amendInsertUndo(index, value.length)

        buffer.insert(index, value)
    }

    fun replace(range: IntRange, replacement: CharSequence) {
        val normalRange = fitRange(range)

        inChangeSet {
            val old = buffer.substring(normalRange)
            undoMan.current.addUndoAction {
                it.buffer.replace(normalRange, old)
                it.cursor = normalRange.first
            }
        }

        buffer.replace(normalRange, replacement.toString())
    }

    fun replace(range: IntRange, transformer: (CharSequence) -> CharSequence) {
        val old = buffer.subSequence(range.first, range.last + 1)
        val new = transformer(old).toString()
        replace(range, new)
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

    override fun deleteRange(startOffset: Int, endOffset: Int): CharSequence {
        val toDelete = this[startOffset, endOffset]
        delete(startOffset..endOffset)
        return toDelete
    }

    override fun insert(text: CharSequence, charOffset: Int) {
        this.insert(charOffset, text)
    }

    override fun set(contents: CharSequence) {
        set(contents.toString(), clampCursor = true)
    }

    override fun startOfLine(charOffset: Int): Int = 0
    override fun endOfLine(charOffset: Int): Int = length
    override fun lineIndex(charOffset: Int): Int = 0
    override fun linesCount(): Int = if (isEmpty()) 0 else 1

    private fun amendBackspaceUndo(index: Int, char: Char) {
        inChangeSet {
            undoMan.current.amendUndo<BackspaceUndoable> {
                when {
                    it.start == -1 -> {
                        it.start = index
                        it.deleted.append(char)
                    }

                    index == it.start - 1 -> {
                        --it.start
                        it.deleted.insert(0, char)
                    }

                    else -> undoMan.current.addUndoAction(BackspaceUndoable(index, char))
                }
            }
        }
    }

    private fun amendInsertUndo(index: Int, length: Int) {
        inChangeSet {
            undoMan.current.amendUndo<InsertUndoable> {
                when {
                    it.start == -1 -> {
                        it.start = index
                        it.length = length
                    }

                    index in it -> it.length += length

                    else -> undoMan.current.addUndoAction(InsertUndoable(index, length))
                }
            }
        }
    }

    private fun fitRange(range: IntRange): IntRange =
        if (range.first >= 0 && range.last < buffer.length) range
        else maxOf(0, range.first)..minOf(buffer.lastIndex, range.last)

    private fun normalizeRange(range: IntRange): Pair<IntRange, Int>? {
        val bufferEnd = maxOf(0, buffer.lastIndex)
        if (range.first < range.last) {
            // forward delete
            val end = (range.last - 1)
            if (end < 0) return null

            return (range.first..end) to minOf(bufferEnd, range.first)
        } else {
            val end = (range.first - 1)
            if (end < 0) return null

            return (range.last..end) to minOf(bufferEnd, range.last)
        }
    }

    /**
     * Undoes an insert action by deleting
     */
    internal class InsertUndoable(
        var start: Int = -1,
        var length: Int = 0
    ) : AmendableUndoable {

        operator fun contains(index: Int) =
            index >= start && index <= start + length

        override fun apply(buffer: InputBuffer) {
            buffer.buffer.delete(start, start + length)
            buffer.cursor = start
        }
    }

    /**
     * Undoes backspace actions by inserting
     */
    internal class BackspaceUndoable(
        var start: Int = -1,
        var deleted: StringBuffer = StringBuffer()
    ) : AmendableUndoable {

        constructor(start: Int, char: Char)
                : this(start, StringBuffer()) {
            deleted.append(char)
        }

        override fun apply(buffer: InputBuffer) {
            buffer.buffer.insert(start, deleted)
            buffer.cursor = start
        }
    }

}

fun StringBuilder.replace(range: IntRange, replacement: String) {
    replace(range.first, range.last + 1, replacement)
}

fun StringBuilder.substring(range: IntRange): String =
    if (range.isEmpty()) ""
    else substring(range.first, range.last + 1)

