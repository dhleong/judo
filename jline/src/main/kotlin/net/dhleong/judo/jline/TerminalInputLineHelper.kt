package net.dhleong.judo.jline

import net.dhleong.judo.MAX_INPUT_LINES
import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.toFlavorable

private val ELLIPSIS = "…".toFlavorable()

/**
 * @author dhleong
 */
class TerminalInputLineHelper(
    private val settings: StateMap,
    var windowWidth: Int = 0,
    private val forcedMaxInputLines: Int? = null
) {

    private val workspace = mutableListOf<FlavorableCharSequence>()

    fun fitInputLinesToWindow(
        input: InputLine,
        output: MutableList<FlavorableCharSequence>
    ) {
        val maxLineWidth = windowWidth
        val maxLines = forcedMaxInputLines ?: settings[MAX_INPUT_LINES]

        val line = input.line
        if (line.length < maxLineWidth || maxLines == 1) {
            // convenient shortcut
            fitInputLineToWindow(input, output)
            return
        }

        workspace.clear()

        val wordWrap = settings[WORD_WRAP]
        line.splitLinesInto(
            workspace,
            maxLineWidth, wordWrap,
            preserveWhitespace = true
        )
        fitCursorInLines(input, workspace)

        if (input.cursorCol == maxLineWidth) {
            input.cursorRow += 1
            input.cursorCol = 0
            workspace.add(FlavorableStringBuilder.EMPTY)
        }

        if (workspace.size <= maxLines) {
            // easy case; cursor position is set above
            output.clear()
            output.addAll(workspace)
            return
        }

        // limit number of lines
        val cursorRow = input.cursorRow
        val cursorCol = input.cursorCol
        var start = maxOf(cursorRow - maxLines / 2, 0)
        val end = minOf(start + maxLines, workspace.size) - 1

        if (end == workspace.lastIndex) {
            // NOTE: end is inclusive, so we don't want end - start = maxLines
            while (end - start < (maxLines - 1) && start > 0) {
                --start
            }
        }

        val first = workspace[start]
        if (start == 0) {
            output.add(first)
        } else {
            output.add(
                FlavorableStringBuilder(first.length).apply {
                    this += ELLIPSIS
                    append(first, 1, first.length)
                }
            )
        }

        // no extra allocations, please
        for (i in (start + 1) until end) {
            output.add(workspace[i])
        }

        val last = workspace[end]
        if (end == workspace.lastIndex) {
            output.add(last)
        } else {
            output.add(
                FlavorableStringBuilder(last.length).apply {
                    append(last, 0, last.length - 1)
                    this += ELLIPSIS
                }
            )
        }

        input.cursorRow = (cursorRow - start)
        input.cursorCol = cursorCol
    }

    private fun fitCursorInLines(
        input: InputLine,
        lines: List<FlavorableCharSequence>
    ) {
        // thanks to word-wrap, cursor row/col is not an exact fit, so just go find it
        var cursorCol = input.cursorIndex
        if (cursorCol == -1) {
            // don't change a -1 cursorIndex (usually for simple status messages)
            input.cursorCol = -1
            return
        }

        val lastLine = lines.lastIndex
        for (row in lines.indices) {
            val rowLen = lines[row].renderLength
            if (cursorCol < rowLen || (row == lastLine && cursorCol == rowLen)) {
                input.cursorRow = row
                input.cursorCol = cursorCol
                return
            }

            cursorCol -= rowLen
        }

        throw IllegalStateException("Couldn't fit cursor ${input.cursorIndex} in ${lines.map { "\"$it\"" }}")
    }

    private fun fitInputLineToWindow(
        input: InputLine,
        output: MutableList<FlavorableCharSequence>
    ) {
        val maxLineWidth = windowWidth
        val line = input.line
        val cursor = input.cursorIndex

        if (line.length < maxLineWidth) {
            // convenient shortcut
            output.add(line)
            input.cursorRow = 0
            input.cursorCol = cursor
            return
        }

        // take the slice of `line` that contains `cursor`
        val absolutePage = cursor / maxLineWidth
        val absolutePageCursor = cursor % maxLineWidth

        // if the cursor fits on the previous visualOffset page,
        // draw that one; else draw the absolute page
        val visualOffset = maxLineWidth / 2
        val windowStart: Int
        val cursorOffset: Int
        if (absolutePage > 0 && absolutePageCursor < visualOffset) {
            windowStart = absolutePage * maxLineWidth - visualOffset
            cursorOffset = visualOffset
        } else {
            windowStart = absolutePage * maxLineWidth
            cursorOffset = 0
        }

        val windowEnd = minOf(line.length, windowStart + maxLineWidth)
        val hasMorePrev = absolutePage > 0
        val hasMoreNext = windowEnd < line.length

        // indicate continued
        val withIndicator: FlavorableCharSequence
        if (!(hasMoreNext || hasMorePrev)) {
            // minor optimization for the common case
            withIndicator = line.subSequence(windowStart, windowEnd)
        } else {
            val windowedInput = line.subSequence(
                if (hasMorePrev) windowStart + 1
                else windowStart,

                if (hasMoreNext) windowEnd - 1
                else windowEnd
            )

            withIndicator = FlavorableStringBuilder(maxLineWidth).apply {
                if (hasMorePrev) this += ELLIPSIS

                this += windowedInput

                if (hasMoreNext) this += ELLIPSIS
            }
        }

        output.add(withIndicator)
        input.cursorRow = 0
        input.cursorCol = when (cursor) {
            -1 -> -1 // see above
            else -> absolutePageCursor + cursorOffset
        }
    }
}