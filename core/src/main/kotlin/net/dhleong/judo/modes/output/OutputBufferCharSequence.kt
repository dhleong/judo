package net.dhleong.judo.modes.output

import net.dhleong.judo.input.IBufferWithCursor
import net.dhleong.judo.modes.output.OutputBufferCharSequence.Companion.CHARS_PER_LINE
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoWindow

/**
 * @author dhleong
 */
class OutputBufferCharSequence(
    private val buffer: IJudoBuffer,
    override var cursor: Int
) : CharSequence, IBufferWithCursor {
    companion object {
        const val CHARS_PER_LINE = 10_000
    }

    constructor(win: IJudoWindow) : this(
        win.currentBuffer,
        win.computeCursor()
    )

    override val length: Int
        get() = buffer.size * CHARS_PER_LINE

    override val size: Int
        get() = length

    override fun toChars(): CharSequence = this

    override fun clear() {
        buffer.clear()
    }

    override fun get(index: Int): Char {
        val lineNr = index / CHARS_PER_LINE
        val col = index % CHARS_PER_LINE
        val line = buffer[lineNr]
        if (col >= line.length) {
            return ' '
        }
        return line[col]
    }

    override fun get(range: IntRange): CharSequence =
        subSequence(range.first, range.last + 1)

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        val startLineNr = startIndex / CHARS_PER_LINE
        val endLineNr = endIndex / CHARS_PER_LINE
        if (startLineNr == endLineNr) {
            // easy case
            val line = buffer[startLineNr]
            val startCol = (startIndex % CHARS_PER_LINE).coerceAtMost(line.length)
            val endCol = (endIndex % CHARS_PER_LINE).coerceAtMost(line.length)
            return line.subSequence(startCol, endCol).trim('\n')
        }

        TODO()
    }

    fun applyCursorTo(win: IJudoWindow) {
        val bufferLine = cursor / CHARS_PER_LINE
        val cursorCol = cursor % CHARS_PER_LINE

        win.setCursorFromBufferLocation(bufferLine, cursorCol)
    }

}

private fun IJudoWindow.computeCursor(): Int {
    val (line, col) = computeCursorLocationInBuffer()
    return line * CHARS_PER_LINE + col
}
