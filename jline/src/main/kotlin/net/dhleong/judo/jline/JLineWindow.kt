package net.dhleong.judo.jline

import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.render.BaseJudoWindow2
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoBuffer2
import net.dhleong.judo.render.IdManager
import org.jline.utils.AttributedString

class JLineWindow(
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
) {

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

        for (i in 0 until blankLinesNeeded) {
            display.withLine(x, y + i) {
                // clear the line
                for (j in 0 until width) {
                    append(" ")
                }
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
        TODO("not implemented")
    }

    override fun getScrollback(): Int = scrollbackBottom

    override fun scrollLines(count: Int) {
        val buffer = currentBuffer
        val width = this.width
        val desired = Math.abs(count)
        val end = buffer.size - 1

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

    override fun scrollToBottom() {
        scrollbackBottom = 0
        searchResultLine = -1
    }

    override fun scrollToBufferLine(line: Int) {
        TODO("not implemented")
    }
}

internal fun FlavorableCharSequence.splitLinesInto(
    target: MutableList<AttributedString>,
    windowWidth: Int,
    wordWrap: Boolean
) {
    forEachRenderedLine(windowWidth, wordWrap) { start, end ->
        target.add(subSequence(start, end).toAttributedString())
    }
}

