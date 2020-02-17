package net.dhleong.judo.jline

import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.inTransaction
import net.dhleong.judo.render.BaseJudoWindow
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.theme.AppColors
import net.dhleong.judo.theme.ColorTheme
import net.dhleong.judo.theme.UiElement
import net.dhleong.judo.theme.get
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import kotlin.math.abs
import kotlin.properties.Delegates

/**
 * @author dhleong
 */
class JLineWindow(
    private val renderer: IJLineRenderer,
    private val ids: IdManager,
    settings: StateMap,
    initialWidth: Int,
    initialHeight: Int,
    initialBuffer: IJudoBuffer,
    isFocusable: Boolean = false,
    statusLineOverlaysOutput: Boolean = false
) : BaseJudoWindow(
    renderer, ids, settings,
    initialWidth, initialHeight,
    isFocusable, statusLineOverlaysOutput
), IJLineWindow {

    override var currentBuffer: IJudoBuffer by Delegates.observable(
        initialBuffer
    ) { _, oldValue, newValue ->
        oldValue.detachWindow(this)
        newValue.attachWindow(this)
    }

    init {
        initialBuffer.attachWindow(this)
    }

    override var isFocused: Boolean = false
    override val visibleHeight
        get() = when {
            isFocusable -> height - 1 // status line
            else -> height
        }

    override var isWindowHidden: Boolean
        get() = super.isWindowHidden
        set(nowHidden) {
            if (nowHidden != isWindowHidden) renderer.inTransaction {
                super.isWindowHidden = nowHidden

                if (!nowHidden) {
                    // re-showing; trigger other windows to resize
                    lastResizeRequest = ids.newTimestamp()
                    renderer.onWindowResized(this)
                }
            }
        }
    override var cursorCol: Int
        get() = super.cursorCol
        set(value) = renderer.inTransaction { super.cursorCol = value }
    override var cursorLine: Int
        get() = super.cursorLine
        set(value) = renderer.inTransaction { super.cursorLine = value }

    override var lastResizeRequest: Long = ids.newTimestamp()

    private var echoLine: FlavorableCharSequence? = null
    private var status = InputLine(cursorIndex = -1)
    private val statusHelper = TerminalInputLineHelper(
        settings,
        forcedMaxInputLines = 1, // TODO can we render a wrapped status line? should we?
        windowWidth = width
    )
    private val statusWorkspace = ArrayList<FlavorableCharSequence>(1)
    override val statusCursor: Int
        get() = status.cursorCol

    /** offset from end of output */
    private var scrollbackBottom = 0

    /** offset within output line # [scrollbackBottom] */
    private var scrollbackOffset = 0

    private var renderWorkspace = ArrayList<AttributedString>(initialHeight + 8)

    override fun render(display: JLineDisplay, x: Int, y: Int) {
        // synchronize on the buffer to protect against concurrent
        // modification
        val buffer = currentBuffer
        synchronized(buffer) {
            renderWith(buffer, display, x, y)
        }
    }

    private fun renderWith(buffer: IJudoBuffer, display: JLineDisplay, x: Int, y: Int) {
        val displayHeight =
            if (isFocusable && !statusLineOverlaysOutput) height - 1 // make room for status line
            else height

        val wordWrap = settings[WORD_WRAP]
        val start = buffer.lastIndex - scrollbackBottom
        val end = maxOf(0, start - displayHeight)

        renderWorkspace.clear()

        val isSearching = search.resultLine > -1
        var workspaceSearchIndex = -1
        val theme = buffer.settings[AppColors]
        val colors = theme?.output

        // iterating from oldest to newest
        for (i in end..start) {
            if (isSearching && i == search.resultLine) {
                workspaceSearchIndex = renderWorkspace.size
            }

            buffer[i].splitAttributedLinesInto(
                renderWorkspace,
                windowWidth = width,
                wordWrap = wordWrap,
                colorTheme = colors
            )
        }

        var displayEnd = renderWorkspace.size - scrollbackOffset
        if (isFocusable && statusLineOverlaysOutput) {
            displayEnd -= 1
        }

        val displayStart = maxOf(0, displayEnd - displayHeight)
        val actualDisplayedLines = maxOf(0, displayEnd - displayStart)

        var blankLinesNeeded = (displayHeight - actualDisplayedLines)
        if (isFocusable && statusLineOverlaysOutput) {
            blankLinesNeeded -= 1
        }

        for (i in 0 until blankLinesNeeded) {
            display.clearLine(
                x, y + i,
                fromRelativeX = 0,
                toRelativeX = width
            )
        }

        // highlight search results:
        // if we're not focusable (EG: primary output window), always highlight
        // results; otherwise, only highlight if actually focused
        if ((!isFocusable || isFocused) && workspaceSearchIndex != -1) {
            val fullLine = buffer[search.resultLine]
            val splitIndex = fullLine.splitIndexOfOffset(
                windowWidth = width,
                wordWrap = wordWrap,
                offset = search.resultOffset
            )
            val lineIndex = workspaceSearchIndex + splitIndex

            if (lineIndex < renderWorkspace.size) {
                val original = renderWorkspace[lineIndex]
                val offsetOnLine = fullLine.splitOffsetOfOffset(
                    windowWidth = width,
                    wordWrap = wordWrap,
                    offset = search.resultOffset
                )

                if (offsetOnLine >= 0) {
                    val wordEnd = offsetOnLine + search.lastKeyword.length
                    val word = original.substring(offsetOnLine, wordEnd)

                    val new = AttributedStringBuilder(original.length)
                    new.append(original, 0, offsetOnLine)
                    new.append(word, AttributedStyle.INVERSE)
                    new.append(original, wordEnd, original.length)
                    val newString = new.toAttributedString()

                    renderWorkspace[lineIndex] = newString
                }
            }
        }

        var line = y + blankLinesNeeded
        for (i in displayStart until displayEnd) {
            val text = renderWorkspace[i]
            display.withLine(x, line, lineWidth = width) {
                append(text)
            }

            ++line
        }

        if (isFocusable && isFocused) {
            val statusLineToRender = echoLine ?: let {
                statusWorkspace.clear()
                statusHelper.fitInputLinesToWindow(status, statusWorkspace)

                // NOTE we assume only ever 1 status output line
                statusWorkspace[0]
            }

            display.withLine(x, line, lineWidth = width) {
                statusLineToRender.appendTo(this, colorTheme = colors)
            }
        } else if (isFocusable) {
            // TODO faded out status? or just a background color?
//            display.clearLine(x, line, 0, width)

            display.withLine(x, line, lineWidth = width) {
                val style = theme[UiElement.Dividers].toAttributedStyle()
                for (i in 0 until width) {
                    append("-", style)
                }
            }
        }
    }

    override fun echo(text: FlavorableCharSequence) {
        if (!isFocusable) throw IllegalStateException("Not-focusable JudoWindow cannot receive echo")
        renderer.inTransaction {
            this.echoLine = text.removeSuffix("\n") as FlavorableCharSequence
        }
    }

    override fun clearEcho() = renderer.inTransaction {
        this.echoLine = null
    }

    override fun updateStatusLine(line: FlavorableCharSequence, cursor: Int) {
        if (!isFocusable) throw IllegalStateException("Not-focusable JudoWindow cannot receive status line")
        renderer.inTransaction {
            echoLine = null
            status.line = line.removeSuffix("\n") as FlavorableCharSequence
            status.cursorIndex = cursor

            // also set this since that's where statusCursor comes from;
            // it'll get recalculated for render anyway, but it's important
            // that statusCursor *at least* not be -1 if [cursor] is not -1
            // (see JLineRenderer.render)
            status.cursorCol = cursor
        }
    }

    override fun resize(width: Int, height: Int) {
        if (this.width == width && this.height == height) {
            // nop
            return
        }

        renderer.inTransaction {
            this.width = width
            this.height = height
            statusHelper.windowWidth = width
            if (renderer.onWindowResized(this)) {
                lastResizeRequest = ids.newTimestamp()
            }
        }
    }

    override fun getScrollback(): Int = scrollbackBottom

    override fun scrollLines(count: Int) = renderer.inTransaction {
        echoLine = null

        val buffer = currentBuffer
        if (buffer.size <= 0) {
            // can't scroll within an empty or single-line buffer
            return
        }

        val width = this.width
        val desired = abs(count)
        val step = count / desired
        val end = buffer.lastIndex

        val rangeStart = end - scrollbackBottom
        val range = IntProgression.fromClosedRange(
            rangeStart = rangeStart,
            rangeEnd = (rangeStart - count).coerceIn(0, end),
            step = -step
        )

        // clear search result on scroll (?)
        // TODO should we do better?
        search.reset()

        val wordWrap = settings[WORD_WRAP]
        var toScroll = desired
        for (lineNr in range) {
            val line = buffer[lineNr]
            val consumableLines = if (count > 0) {
                val renderedLines = line.computeRenderedLinesCount(width, wordWrap)
                renderedLines - scrollbackOffset
            } else {
                // NOTE: there's always at least one line to consume when scrolling backward
                scrollbackOffset + 1
            }

            // scroll within a wrapped line
            // NOTE: we consume this now in case this is the last line but
            //  we wanted more
            scrollbackOffset += step * toScroll.coerceAtMost(consumableLines)
            if (scrollbackOffset < 0) scrollbackOffset = 0
            if (toScroll < consumableLines) {
                // done!
                break
            }

            if (count > 0 && scrollbackBottom + 1 > end) {
                // end of the buffer and still scrolling; cancel
                break
            } else if (count < 0 && scrollbackBottom - 1 < 0) {
                break
            }

            toScroll -= consumableLines
            scrollbackBottom += step
            scrollbackOffset =
                if (count > 0) 0
                else buffer[end - scrollbackBottom].computeRenderedLinesCount(width, wordWrap) - 1

            // finish scrolling past a wrapped line
            if (toScroll == 0) break
        }

        if (scrollbackBottom == end) {
            // last buffer line; ensure we don't offset-scroll it
            // out of visible range
            val line = buffer[end]
            val renderedLines = line.computeRenderedLinesCount(width, wordWrap)
            scrollbackOffset = scrollbackOffset.coerceIn(0, renderedLines - step)
        }
    }

    override fun scrollToBottom() = renderer.inTransaction {
        scrollbackBottom = 0
        scrollbackOffset = 0
        search.reset()
    }

    override fun scrollToBufferLine(line: Int, offsetOnLine: Int) = renderer.inTransaction {
        val buffer = currentBuffer
        val wordWrap = settings[WORD_WRAP]

        // is the line actually visible already?
        val lastVisibleLine = buffer.lastIndex - scrollbackBottom
        val firstPossiblyVisibleLine = maxOf(0, lastVisibleLine - visibleHeight + 1)
        if (line >= firstPossiblyVisibleLine) {
            // it's *possible*, but let's take into account wrapped lines
            var lines = visibleHeight + scrollbackOffset
            for (i in lastVisibleLine downTo firstPossiblyVisibleLine) {
                val renderedLines = buffer[i].computeRenderedLinesCount(width, wordWrap)
                if (i == line) {
                    // now check against the offset
                    val actualIndex = buffer[i].splitIndexOfOffset(width, wordWrap, offsetOnLine)
                    val linesNeededToSeeOffset = renderedLines - actualIndex

                    val isPastWindow = i == lastVisibleLine
                        && actualIndex >= renderedLines - scrollbackOffset
                    if (isPastWindow) {
                        // shortcut out
                        break
                    }

                    if (lines >= linesNeededToSeeOffset) {
                        // huzzah!
                        return@inTransaction
                    }
                }

                lines -= renderedLines
                if (lines <= 0) break
            }
        }

        scrollbackBottom = buffer.lastIndex - line

        var renderedLines = 0
        var foundOnLine = -1
        buffer[line].forEachRenderedLine(width, wordWrap) { start, end ->
            if (offsetOnLine in start until end) {
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

    override fun onBufModifyPre(): Any? {
        val b = currentBuffer
        val wordWrap = settings[WORD_WRAP]

        renderer.beginUpdate()
        return ScrollAdjustmentState(
            linesBefore = b.size,
            lastLineRenderHeight = when {
                b.size > 0 -> b[b.lastIndex].computeRenderedLinesCount(width, wordWrap)
                else -> 0
            }
        )
    }

    override fun onBufModifyPost(preState: Any?) {
        try {
            val b = currentBuffer
            val wordWrap = settings[WORD_WRAP]

            if (b.size == 0) {
                // easy case
                scrollbackBottom = 0
                scrollbackOffset = 0
                return
            }

            val (linesBefore, lastLineRenderHeight) = preState as ScrollAdjustmentState
            if (scrollbackBottom == 0 && scrollbackOffset == 0) {
                // no need to maintain scroll position
                return
            }
            if (linesBefore == 0) {
                // could not have scrolled; quick reject
                return
            }

            val linesAfter = b.size

            if (linesBefore == linesAfter) {
                // appending to last line
                val newRenderHeight = b[b.lastIndex].computeRenderedLinesCount(width, wordWrap)
                scrollbackOffset += (newRenderHeight - lastLineRenderHeight)
            } else {
                scrollbackBottom += linesAfter - linesBefore
            }

            if (scrollbackBottom < 0) {
                scrollbackBottom = 0
                scrollbackOffset = 0
            }
        } finally {
            renderer.finishUpdate()
        }
    }

    fun measureRenderedLines(width: Int): Int {
        val buffer = currentBuffer
        val wordWrap = settings[WORD_WRAP]

        var lines = 0
        for (i in 0..buffer.lastIndex) {
            val line = buffer[i]
            lines += line.computeRenderedLinesCount(width, wordWrap)
        }

        return lines
    }

    override fun toString() = "JLineWindow(id=$id)"
}

internal fun FlavorableCharSequence.splitAttributedLinesInto(
    target: MutableList<AttributedString>,
    windowWidth: Int,
    wordWrap: Boolean,
    colorTheme: ColorTheme? = null
) {
    forEachRenderedLine(windowWidth, wordWrap) { start, end ->
        target.add(subSequence(start, end).toAttributedString(
            widthForTrailingFlavor = windowWidth,
            colorTheme = colorTheme
        ))
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

private data class ScrollAdjustmentState(
    val linesBefore: Int,
    val lastLineRenderHeight: Int
)