package net.dhleong.judo.modes.output

import net.dhleong.judo.input.TextDocument
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoBuffer
import java.lang.UnsupportedOperationException

/**
 * @author dhleong
 */
class OutputBufferTextDocument(
    private val buffer: IJudoBuffer,
    override var cursor: Int
) : TextDocument {

    override val length: Int
        get() = charsBeforeLine(buffer.size)

    override fun get(charOffset: Int): Char {
        val line = lineIndexForOffset(charOffset)
        val col = charOffset - startOfLine(charOffset)
        return buffer[line][col]
    }

    override fun get(startOffset: Int, endOffset: Int): CharSequence {
        val startLine = lineIndexForOffset(startOffset)
        val startCol = startOffset - startOfLine(startOffset)

        val endLine = lineIndexForOffset(endOffset)
        val endCol = endOffset - startOfLine(endOffset)

        if (startLine == endLine) {
            return buffer[startLine].subSequence(startCol, endCol)
        }

        return FlavorableStringBuilder(endOffset - startOffset).apply {
            append(buffer[startLine], startCol)
            for (i in (startLine + 1) until endLine) {
                append(buffer[i])
            }
            append(buffer[endLine], 0, endCol)
        }
    }

    override fun isEmpty(): Boolean = buffer.size > 0

    override fun startOfLine(charOffset: Int): Int {
        val line = lineIndexForOffset(charOffset)
        return charsBeforeLine(line)
    }

    override fun endOfLine(charOffset: Int): Int {
        val line = lineIndexForOffset(charOffset)
        return startOfLine(charOffset) + buffer[line].length
    }

    override fun lineIndex(charOffset: Int): Int = lineIndexForOffset(charOffset)

    override fun linesCount(): Int = buffer.size

    override fun clear() {
        buffer.clear()
    }

    override fun deleteRange(startOffset: Int, endOffset: Int): CharSequence {
        throw UnsupportedOperationException("OutputBuffer may not be edited")
    }

    override fun insert(text: CharSequence, charOffset: Int) {
        throw UnsupportedOperationException("OutputBuffer may not be edited")
    }

    override fun set(contents: CharSequence) {
        throw UnsupportedOperationException("OutputBuffer may not be edited")
    }

    private fun charsBeforeLine(lineIndex: Int): Int {
        // TODO the Buffer probably ought to do some of the heavy lifting
        //  here, to avoid having to page in the entire persisted history...
        return (0..lineIndex).sumBy { buffer[it].length }
    }

    private fun lineIndexForOffset(charOffset: Int): Int {
        // TODO the Buffer probably ought to do some of the heavy lifting
        //  here, to avoid having to page in the entire persisted history...
        var charsSeen = 0
        for (lineIndex in 0 until buffer.size) {
            charsSeen += buffer[lineIndex].length
            if (charsSeen >= charOffset) {
                return lineIndex
            }
        }
        return 0
    }

}