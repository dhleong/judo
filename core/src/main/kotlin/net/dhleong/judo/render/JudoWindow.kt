package net.dhleong.judo.render

import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.util.ansi
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder

/**
 * @author dhleong
 */
class JudoWindow(
    ids: IdManager,
    private val settings: StateMap,
    initialWidth: Int,
    initialHeight: Int,
    initialBuffer: IJudoBuffer,
    override val isFocusable: Boolean = false,
    val statusLineOverlaysOutput: Boolean = false
) : IJudoWindow {

    override val id = ids.newWindow()
    override var width: Int = initialWidth
    override var height: Int = initialHeight

    override var currentBuffer: IJudoBuffer = initialBuffer
    override var isFocused: Boolean = false

    private var displayWorkspace = ArrayList<AttributedString>(initialHeight + 8)

    /** offset from end of output */
    private var scrollbackBottom = 0

    /** offset within output[scrollbackBottom] */
    private var scrollbackOffset = 0

    private var lastSearchKeyword: String = ""
    private var searchResultLine = -1

    private var status = AttributedString.EMPTY
    override var statusCursor: Int = -1

    override fun appendLine(line: CharSequence, isPartialLine: Boolean): CharSequence {
        val buffer = currentBuffer
        val previousLength = buffer.size
        val wordWrap = settings[WORD_WRAP]
        val result = buffer.appendLine(line, isPartialLine, width, wordWrap)
        val linesAdded = buffer.size - previousLength

        val atBottom = scrollbackBottom == 0 && scrollbackOffset == 0
        if (!atBottom) {
            scrollbackBottom += linesAdded
        }

        return result
    }

    override fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    override fun getDisplayLines(lines: MutableList<CharSequence>) {
        val displayHeight =
            if (isFocusable && !statusLineOverlaysOutput) height - 1 // make room for status line
            else height

        val buffer = currentBuffer
        val wordWrap = settings[WORD_WRAP]
        val start = buffer.lastIndex - scrollbackBottom
        val end = maxOf(0, start - displayHeight)

        displayWorkspace.clear()

        // iterating from oldest to newest
        @Suppress("LoopToCallChain") // no extra allocations please, thanks
        for (i in end..start) {
            val line = buffer[i] as OutputLine
            displayWorkspace.addAll(
                line.getDisplayLines(width, wordWrap)
            )
        }

        var displayEnd = displayWorkspace.size - scrollbackOffset
        if (isFocusable && isFocused && statusLineOverlaysOutput) {
            displayEnd -= 1
        }

        val displayStart = maxOf(0, displayEnd - displayHeight)
        val actualDisplayedLines = maxOf(0, displayEnd - displayStart)

        var blankLinesNeeded = (displayHeight - actualDisplayedLines)
        if (isFocusable && isFocused && statusLineOverlaysOutput) {
            blankLinesNeeded -= 1
        }

        val listStart = lines.size
        for (i in 1..blankLinesNeeded) {
            // add blank lines to fill the window
            lines.add(AttributedString.EMPTY)
        }

        @Suppress("LoopToCallChain") // no extra allocations please, thanks
        for (i in displayStart..displayEnd - 1) {
            lines.add(displayWorkspace[i])
        }

        if (searchResultLine >= 0) {
            val lineIndex = listStart + searchResultLine
            val original = lines[lineIndex]
            val wordStart = original.indexOf(lastSearchKeyword, ignoreCase = true)
            if (wordStart >= 0) {
                val wordEnd = wordStart + lastSearchKeyword.length
                val word = original.substring(wordStart, wordEnd)
                val highlighted = "${ansi(inverse = true)}$word${ansi(0)}"

                val new = AttributedStringBuilder(original.length)
                new.append(original, 0, wordStart)
                new.appendAnsi(highlighted)
                new.append(original, wordEnd, original.length)
                val newString = new.toAttributedString()

                lines[lineIndex] = newString
            }
        }

        if (isFocusable && isFocused) {
            lines.add(status)
        } else if (isFocusable && !statusLineOverlaysOutput) {
            // TODO faded out status?
            lines.add(AttributedString.EMPTY)
        }
    }

    override fun getScrollback(): Int = scrollbackBottom

    override fun updateStatusLine(line: CharSequence, cursor: Int) {
        if (!isFocusable) throw IllegalStateException("Not-focusable JudoWindow cannot receive status line")
        status = AttributedString.fromAnsi(line.toString())
        statusCursor = cursor
    }

    @Synchronized override fun scrollLines(count: Int) {
        // take into account wrapped lines
        val output = currentBuffer
        val width = this.width
        val desired = Math.abs(count)
        val step = count / desired
        val end = output.size - 1

        val rangeEnd = minOf(end, scrollbackBottom + count)
        val range =
            if (count > 0) scrollbackBottom..rangeEnd
            else scrollbackBottom downTo rangeEnd

        // clear search result on scroll (?)
        // TODO should we do better?
        searchResultLine = -1

        val wordWrap = settings[WORD_WRAP]
        var scrolled = 0
        for (i in range) {
            if (i < 0) break
            if (i >= output.size) break

            scrollbackBottom = i
            val line = output[end - i] as OutputLine
            val displayedLines = line.getDisplayedLinesCount(width, wordWrap)
            val renderedLines = displayedLines - scrollbackOffset
            val newScrolled = scrolled + renderedLines
            if (newScrolled == desired) {
                // exactly where we want to be, no offset necessary
                scrollbackBottom = maxOf(
                    0,
                    minOf(
                        end,
                        scrollbackBottom + step
                    )
                )
                scrollbackOffset = 0
                break
            } else if (newScrolled > desired) {
                // the logical line had too many visual lines;
                // offset into it
                scrollbackOffset += step * (desired - scrolled)
                if (scrollbackOffset < 0) {
                    // scrolling backwards, so this was a reverse offset
                    scrollbackOffset += displayedLines
                }
                break
            } else if (newScrolled < desired
                    && (step > 0 && scrollbackBottom == end
                        || step < 0 && scrollbackBottom == 0)) {
                // end of the line; just stop
                break
            }

            scrolled = newScrolled
            scrollbackOffset = 0 // we've moved on; reset the offset
        }
    }

    override fun scrollPages(count: Int) {
        scrollLines(height * count)
    }

    override fun scrollToBottom() {
        scrollbackBottom = 0
        searchResultLine = -1
    }

    override fun searchForKeyword(word: CharSequence, direction: Int) {
        val originalSearchResultLine = searchResultLine
        val originalScrollbackBottom = scrollbackBottom
        val originalScrollbackOffset = scrollbackOffset

        if (word != lastSearchKeyword) {
            lastSearchKeyword = word.toString()
            searchResultLine = -1
        }

        val lines = mutableListOf<CharSequence>()
        do {
            val lastScrollbackBottom = scrollbackBottom
            val lastScrollbackOffset = scrollbackOffset

            lines.clear()
            getDisplayLines(lines)
            val last =
                if (direction > 0) lines.lastIndex
                else 0
            val iterateStart =
                if (searchResultLine == -1) last
                else searchResultLine - direction

            if (iterateStart >= 0) {
                val range =
                    if (direction > 0) iterateStart downTo 0
                    else iterateStart..lines.lastIndex
                for (i in range) {
                    val line = lines[i]
                    if (line.contains(word, true)) {
                        searchResultLine = i
                        return
                    }
                }
            }

            scrollPages(direction)
        } while (searchResultLine == -1
            && (scrollbackBottom > lastScrollbackBottom || scrollbackOffset != lastScrollbackOffset))

        // couldn't find anything; reset position
        scrollbackBottom = originalScrollbackBottom
        scrollbackOffset = originalScrollbackOffset

        if (originalSearchResultLine != -1) {
            searchResultLine = originalSearchResultLine
        }

        // TODO bell? echo?
        appendLine("Pattern not found: $word", isPartialLine = false)
    }
}

/**
 * Special window that delegates to two separate windows, one
 * containing the primary output buffer for a Connection, and
 * one containing prompts extracted for that Connection.
 */
class PrimaryJudoWindow(
    ids: IdManager,
    settings: StateMap,
    outputBuffer: IJudoBuffer,
    initialWidth: Int,
    initialHeight: Int
) : IJudoWindow {

    override val id = ids.newWindow()
    override var width = initialWidth
    override var height = initialHeight
    override var isFocused: Boolean
        get() = promptWindow.isFocused
        set(value) {
            promptWindow.isFocused = value
        }
    override val isFocusable = true // primary window is ALWAYS focusable

    override var currentBuffer: IJudoBuffer
        get() = outputWindow.currentBuffer
        set(value) {
            outputWindow.currentBuffer = value
        }

    override val statusCursor: Int
        get() = promptWindow.statusCursor

    val outputWindow = JudoWindow(ids, settings,
        initialWidth, initialHeight - 1,
        outputBuffer,
        isFocusable = false)

    val promptBuffer = JudoBuffer(ids)
    val promptWindow = JudoWindow(ids, settings,
        initialWidth, 1,
        promptBuffer,
        isFocusable = true,
        statusLineOverlaysOutput = true)

    override fun updateStatusLine(line: CharSequence, cursor: Int) =
        promptWindow.updateStatusLine(line, cursor)

    override fun appendLine(line: CharSequence, isPartialLine: Boolean): CharSequence =
        outputWindow.appendLine(line, isPartialLine)

    override fun resize(width: Int, height: Int) {
        val availableHeight = height - promptWindow.height
        outputWindow.resize(width, availableHeight)
        this.width = width
        this.height = height
    }

    override fun getDisplayLines(lines: MutableList<CharSequence>) {
        // easy
        outputWindow.getDisplayLines(lines)
        promptWindow.getDisplayLines(lines)
    }

    fun setPromptHeight(promptHeight: Int) {
        promptWindow.height = maxOf(1, promptHeight)
        resize(width, height)
    }

    override fun getScrollback(): Int = outputWindow.getScrollback()
    override fun scrollLines(count: Int) = outputWindow.scrollLines(count)
    override fun scrollPages(count: Int) = outputWindow.scrollPages(count)
    override fun scrollToBottom() = outputWindow.scrollToBottom()
    override fun searchForKeyword(word: CharSequence, direction: Int) =
        outputWindow.searchForKeyword(word, direction)
}