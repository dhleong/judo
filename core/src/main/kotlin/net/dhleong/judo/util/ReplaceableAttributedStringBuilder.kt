package net.dhleong.judo.util

import org.jline.utils.AttributedStringBuilder

/**
 * @author dhleong
 */
class ReplaceableAttributedStringBuilder(capacity: Int)
        : AttributedStringBuilder(capacity), IStringBuilder {

    constructor(ansiString: String) : this(ansiString.length) {
        appendAnsi(ansiString)
    }

    override fun replace(start: Int, end: Int, str: String) {
        var actualEnd = end

        if (start < 0)
            throw StringIndexOutOfBoundsException(start)
        if (start > length)
            throw StringIndexOutOfBoundsException("start > length()")
        if (start > actualEnd)
            throw StringIndexOutOfBoundsException("start > end")

        if (actualEnd > length)
            actualEnd = length
        val len = str.length
        val newCount = length + len - (actualEnd - start)
        ensureCapacity(newCount)
        val buffer = buffer()

        System.arraycopy(buffer, actualEnd, buffer, start + len, length - actualEnd)
        str.toCharArray(buffer, start)
        setLength(newCount)
    }

    override fun toAnsiString(): String =
        toAnsi()
}
