package net.dhleong.judo.render

import net.dhleong.judo.util.ESCAPE_CODE_SEARCH_LIMIT
import net.dhleong.judo.util.IStringBuilder
import org.jline.utils.AttributedCharSequence
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder

/**
 * @author dhleong
 */
class ReplaceableAttributedStringBuilder(capacity: Int)
        : AttributedStringBuilder(capacity), IStringBuilder {

    internal var postEscapePartials: StringBuilder? = null

    constructor(ansiString: String) : this(ansiString.length) {
        appendAnsi(ansiString)

        preserveHangingAnsi(ansiString)
    }

    override fun isDiscardable(): Boolean = postEscapePartials == null

    /**
     * NOTE: you should generally prefer slice if possible. We would LIKE to
     *  just override this method instead of adding a new one, but JLine
     *  overrode the return type with AttributedString :\
     * We *could* add another subclass of AttributedString that contains
     *  postEscapePartials, but.....
     */
    override fun subSequence(startIndex: Int, endIndex: Int): AttributedString =
        super.subSequence(startIndex, endIndex)

    override fun slice(startIndex: Int, endIndex: Int): AttributedCharSequence {
        if (endIndex < this.lastIndex || postEscapePartials == null) {
            return super.subSequence(startIndex, endIndex)
        } else {
            val partials = postEscapePartials
            return ReplaceableAttributedStringBuilder(endIndex - startIndex).apply {
                append(super.subSequence(startIndex, endIndex))
                postEscapePartials = partials
            }
        }
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

    private fun preserveHangingAnsi(ansiString: CharSequence) {
        if (this.isEmpty() && !ansiString.isEmpty()) {
            // all ansi?
            postEscapePartials = StringBuilder(ansiString)
            return
        } else if (this.isEmpty()) {
            // both empty...?
            return
        }

        // as long as the string char != this[lastIndex], it
        // was stripped as a partial ansi code.
        val lastNonAnsi = this.last()
        var i = ansiString.length
        while (i > 0 && ansiString[i - 1] != lastNonAnsi) {
            --i
        }

        if (i < ansiString.length) {
            postEscapePartials = StringBuilder(ESCAPE_CODE_SEARCH_LIMIT)
                .append(ansiString, i, ansiString.length)
        }
    }
}

