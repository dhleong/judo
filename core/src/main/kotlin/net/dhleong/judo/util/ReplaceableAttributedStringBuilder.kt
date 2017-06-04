package net.dhleong.judo.util

import org.jline.utils.AttributedStringBuilder

/**
 * @author dhleong
 */
class ReplaceableAttributedStringBuilder(capacity: Int)
        : AttributedStringBuilder(capacity), IStringBuilder {

    override fun replace(start: Int, end: Int, str: String) {
        val buffer = buffer()
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

        System.arraycopy(buffer, actualEnd, buffer, start + len, length - actualEnd)
        str.toCharArray(buffer, start)
        setLength(newCount)
    }
}
