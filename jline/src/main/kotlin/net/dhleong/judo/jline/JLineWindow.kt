package net.dhleong.judo.jline

import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.inTransaction
import net.dhleong.judo.render.BaseJudoWindow2
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoBuffer2
import net.dhleong.judo.render.IdManager
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

/**
 * @author dhleong
 */
class JLineWindow(
    private val renderer: JLineRenderer2,
    ids: IdManager,
    settings: StateMap,
    initialWidth: Int,
    initialHeight: Int,
    initialBuffer: IJudoBuffer2,
    isFocusable: Boolean = false,
    statusLineOverlaysOutput: Boolean = false
) : BaseJudoWindow2(
    ids, settings,
    initialWidth, initialHeight, initialBuffer,
    isFocusable, statusLineOverlaysOutput
), IJLineWindow {

    override var currentBuffer: IJudoBuffer2 = initialBuffer
    override var isFocused: Boolean = false

    private var status: CharSequence = ""
    override var statusCursor: Int = -1

    /** offset from end of output */
    private var scrollbackBottom = 0

    /** offset within output line # [scrollbackBottom] */
    private var scrollbackOffset = 0

    private var renderWorkspace = ArrayList<AttributedString>(initialHeight + 8)

    override fun render(display: JLineDisplay, x: Int, y: Int) {
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
            buffer[i].splitAttributedLinesInto(
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

        for (i in 0 until blankLinesNeeded) {
            display.withLine(x, y + i) {
                // clear the line
                for (j in 0 until width) {
                    append(" ")
                }
            }
        }

        // highlight search results:
        if (searchResultLine >= 0) {
            val lineIndex = displayStart - searchResultLine
            val original = renderWorkspace[lineIndex]
            val wordStart = original.indexOf(lastSearchKeyword, ignoreCase = true)
            if (wordStart >= 0) {
                val wordEnd = wordStart + lastSearchKeyword.length
                val word = original.substring(wordStart, wordEnd)

                val new = AttributedStringBuilder(original.length)
                new.append(original, 0, wordStart)
                new.append(word, AttributedStyle.INVERSE)
                new.append(original, wordEnd, original.length)
                val newString = new.toAttributedString()

                renderWorkspace[lineIndex] = newString
            }
        }

        var line = y + blankLinesNeeded
        for (i in displayStart until displayEnd) {
            display.withLine(x, line) {
                append(renderWorkspace[i])

                // fill remaining width with blank space
                for (j in renderWorkspace[i].length until width) {
                    append(" ")
                }
            }

            ++line
        }

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
        renderer.inTransaction {
            status = line.toString()
            statusCursor = cursor
        }
    }

    override fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    override fun getScrollback(): Int = scrollbackBottom

    override fun scrollLines(count: Int) = renderer.inTransaction {
        val buffer = currentBuffer
        val width = this.width
        val desired = Math.abs(count)
        val end = buffer.lastIndex

        val rangeEnd = minOf(end, scrollbackBottom + count)
        val range =
            if (count > 0) scrollbackBottom..rangeEnd
            else scrollbackBottom downTo rangeEnd

        // clear search result on scroll (?)
        // TODO should we do better?
        searchResultLine = -1

        val wordWrap = settings[WORD_WRAP]
        var toScroll = desired
        for (lineNr in range) {
            if (lineNr < 0) break
            if (lineNr >= buffer.size) break

            val line = buffer[lineNr]
            val renderedLines = line.computeRenderedLinesCount(width, wordWrap)
            toScroll -= renderedLines - scrollbackOffset
            if (toScroll < 0) {
                // scroll within a wrapped line
                break
            }

            ++scrollbackBottom
            scrollbackOffset = 0

            // finish scrolling past a wrapped line
            if (toScroll == 0) break
        }

        scrollbackOffset = -toScroll
    }

    override fun scrollToBottom() = renderer.inTransaction {
        scrollbackBottom = 0
        searchResultLine = -1
    }

    override fun scrollToBufferLine(line: Int, offsetOnLine: Int) = renderer.inTransaction {
        // TODO do better than this?

        val buffer = currentBuffer
        scrollbackBottom = buffer.lastIndex - line

        var renderedLines = 0
        var foundOnLine = -1
        buffer[line].forEachRenderedLine(width, settings[WORD_WRAP]) { start, end ->
            if (offsetOnLine in start..(end - 1)) {
                foundOnLine = renderedLines
            }

            ++renderedLines
        }

        scrollbackOffset = when (foundOnLine) {
            // not found? just go *somewhere* on the line
            -1 -> 0

            else -> renderedLines - 1 - foundOnLine
        }
    }
}

internal fun FlavorableCharSequence.splitAttributedLinesInto(
    target: MutableList<AttributedString>,
    windowWidth: Int,
    wordWrap: Boolean
) {
    forEachRenderedLine(windowWidth, wordWrap) { start, end ->
        target.add(subSequence(start, end).toAttributedString())
    }
}

internal fun FlavorableCharSequence.splitLinesInto(
    target: MutableList<FlavorableCharSequence>,
    windowWidth: Int,
    wordWrap: Boolean,
    preserveWhitespace: Boolean = false
) {
    forEachRenderedLine(
        windowWidth,
        wordWrap,
        preserveWhitespace
    ) { start, end ->
        target.add(subSequence(start, end))
    }
}
