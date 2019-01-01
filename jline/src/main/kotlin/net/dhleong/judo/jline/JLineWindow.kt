package net.dhleong.judo.jline

import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoBuffer2
import net.dhleong.judo.render.IJudoWindow2
import net.dhleong.judo.render.IdManager
import org.jline.utils.AttributedCharSequence
import org.jline.utils.AttributedString
import org.jline.utils.WCWidth

class JLineWindow(
    ids: IdManager,
    private val settings: StateMap,
    initialWidth: Int,
    initialHeight: Int,
    initialBuffer: IJudoBuffer2,
    override val isFocusable: Boolean = false,
    val statusLineOverlaysOutput: Boolean = false
) : IJudoWindow2 {

    override val id = ids.newWindow()
    override var width: Int = initialWidth
    override var height: Int = initialHeight

    override var currentBuffer: IJudoBuffer2 = initialBuffer
    override var isFocused: Boolean = false

    private var status: CharSequence = ""
    override var statusCursor: Int = -1

    /** offset from end of output */
    private var scrollbackBottom = 0

    /** offset within output line # [scrollbackBottom] */
    private var scrollbackOffset = 0

    private var renderWorkspace = ArrayList<AttributedString>(initialHeight + 8)

    fun render(display: JLineDisplay, x: Int, y: Int) {
        val displayHeight =
            if (isFocusable && !statusLineOverlaysOutput) height - 1 // make room for status line
            else height

        val buffer = currentBuffer
        val wordWrap = settings[WORD_WRAP]
        val start = buffer.lastIndex - scrollbackBottom
        val end = maxOf(0, start - displayHeight)

        renderWorkspace.clear()

        // iterating from oldest to newest
        for (i in end..start) {
//            val line =
            buffer[i].splitLinesInto(
                renderWorkspace,
                windowWidth = width,
                wordWrap = wordWrap
            )
        }

        var displayEnd = renderWorkspace.size - scrollbackOffset
        if (isFocusable && isFocused && statusLineOverlaysOutput) {
            displayEnd -= 1
        }

        val displayStart = maxOf(0, displayEnd - displayHeight)
        val actualDisplayedLines = maxOf(0, displayEnd - displayStart)

        var blankLinesNeeded = (displayHeight - actualDisplayedLines)
        if (isFocusable && isFocused && statusLineOverlaysOutput) {
            blankLinesNeeded -= 1
        }

        val listStart = y
        var line = y
        for (i in displayStart until displayEnd) {
            display.withLine(x, line) {
                // TODO probably, clear line to end of width?
                append(renderWorkspace[i])
            }

            ++line
        }

        // TODO highlight search results:
//        if (searchResultLine >= 0) {
//        }

        if (isFocusable && isFocused) {
            display.withLine(x, line) {
                append(status)
            }
        } else if (isFocusable && !statusLineOverlaysOutput) {
            // TODO faded out status?
        }
    }

    override fun updateStatusLine(line: CharSequence, cursor: Int) {
        if (!isFocusable) throw IllegalStateException("Not-focusable JudoWindow cannot receive status line")
        status = line.toString()
        statusCursor = cursor
    }

    override fun resize(width: Int, height: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getScrollback(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun scrollLines(count: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun scrollPages(count: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun scrollToBottom() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun searchForKeyword(word: CharSequence, direction: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

internal fun FlavorableCharSequence.splitLinesInto(
    target: MutableList<AttributedString>,
    windowWidth: Int,
    wordWrap: Boolean
) {
    val attributed = toAttributedString()
    if (wordWrap) {
        attributed.wordWrapInto(windowWidth, target)
    } else {
        target.addAll(
            attributed.columnSplitLength(windowWidth)
        )
    }
}

private fun AttributedCharSequence.wordWrapInto(windowWidth: Int, out: MutableList<AttributedString>) {
    val attrLength = length
    var start = 0
    var end = 0
    var col = 0

    while (end < attrLength) {
        val cp = codePointAt(end)
        val charWidth =
            if (isHidden(end)) 0
            else WCWidth.wcwidth(cp)

        when {
            cp == '\n'.toInt() -> {
                out.add(subSequence(start, end))
                start = end + 1
                col = 0
            }

            col + charWidth > windowWidth -> {
                // WRAP! search back for the previous word boundary (if necessary)
                val atBoundary = end == attrLength - 1 || Character.isWhitespace(cp)
                val boundary = when {
                    (atBoundary && Character.isWhitespace(cp)) -> -1

                    else -> findLastBefore(end) { Character.isWhitespace(it) }
                }

                if (boundary != -1 && boundary > start) {
                    // found it!
                    // end *after* the whitespace
                    end = boundary + 1
                    col =
                            if (isHidden(end)) 0
                            else WCWidth.wcwidth(codePointAt(end))

                } else {
                    // if we didn't find any usable boundary, just do a hard split
                    // (or if this word ends RIGHT AT a boundary)
                    col = charWidth
                }

                out.add(subSequence(start, end))
                start = end
            }

            else -> col += charWidth
        }

        end += Character.charCount(cp)
    }

    out.add(subSequence(start, end))
}

private inline fun CharSequence.findLastBefore(beforeIndex: Int, predicate: (Char) -> Boolean): Int {
    var i = beforeIndex - 1
    while (i >= 0) {

        if (predicate(this[i])) {
            // found it!
            return i
        }

        --i
    }

    return -1
}
