package net.dhleong.judo

import net.dhleong.judo.render.OutputLine
import net.dhleong.judo.util.CircularArrayList
import net.dhleong.judo.util.ansi
import org.jline.terminal.Attributes
import org.jline.terminal.Attributes.*
import org.jline.terminal.MouseEvent
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.impl.DumbTerminal
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.Curses
import org.jline.utils.Display
import org.jline.utils.InfoCmp
import org.jline.utils.NonBlockingReader
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.IOException
import java.io.StringWriter
import java.util.EnumSet
import javax.swing.KeyStroke




/**
 * @author dhleong
 */

// ascii codes:
val KEY_ESCAPE = 27
val KEY_DELETE = 127

// this is returned by read() when it times out
val KEY_TIMEOUT = -2

class JLineRenderer(
    val enableMouse: Boolean = false
) : JudoRenderer, BlockingKeySource {

    private val DEFAULT_SCROLLBACK_SIZE = 20_000

    override val terminalType: String
        get() = terminal.type
    override val capabilities: EnumSet<JudoRendererInfo.Capabilities>

    override var onResized: OnResizedEvent? = null

    private val terminal = TerminalBuilder.terminal()!!
    private val window = Display(terminal, true)

    override var windowHeight = -1
    override var windowWidth = -1
    internal var outputWindowHeight = -1
    private val windowSize = Size(0, 0)

    private val output = CircularArrayList<OutputLine>(DEFAULT_SCROLLBACK_SIZE)

    /** offset from end of output */
    private var scrollbackBottom = 0

    /** offset within output[scrollbackBottom] */
    private var scrollbackOffset = 0

    private var lastSearchKeyword: String = ""
    private var searchResultLine = -1

    private var hadPartialLine = false

    private var input = AttributedString.EMPTY
    private var status = AttributedString.EMPTY
    private var cursor = 0
    private var isCursorOnStatus = false

    private val workspace = mutableListOf<AttributedString>()
    private val originalAttributes: Attributes

    private var isInTransaction = false

    private val escapeSequenceHandlers = HashMap<Int, HashMap<Int, () -> KeyStroke?>>(8)

    init {
        terminal.handle(Terminal.Signal.WINCH, this::handleSignal)
        terminal.handle(Terminal.Signal.CONT, this::handleSignal)

        terminal.enterRawMode()

        originalAttributes = terminal.attributes
        val newAttr = Attributes(originalAttributes)
        newAttr.setLocalFlags(EnumSet.of(LocalFlag.ICANON, LocalFlag.ECHO, LocalFlag.IEXTEN), false)
        newAttr.setInputFlags(EnumSet.of(InputFlag.IXON, InputFlag.ICRNL, InputFlag.INLCR), false)
        newAttr.setControlChar(ControlChar.VMIN, 1)
        newAttr.setControlChar(ControlChar.VTIME, 0)
        newAttr.setControlChar(ControlChar.VINTR, 0)
        terminal.attributes = newAttr

        terminal.puts(InfoCmp.Capability.enter_ca_mode)
        terminal.puts(InfoCmp.Capability.keypad_xmit)

        if (enableMouse) {
            terminal.trackMouse(Terminal.MouseTracking.Normal)
        }

        terminal.flush()
        resize()

        // register handlers for keystrokes that are done via escape sequences
        registerEscapeHandler(InfoCmp.Capability.key_mouse, this::readMouseEvent)
        registerEscapeHandler(InfoCmp.Capability.key_btab) {
            KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)
        }
        registerEscapeHandler(InfoCmp.Capability.key_down) {
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)
        }
        registerEscapeHandler(InfoCmp.Capability.key_up) {
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)
        }
        registerEscapeHandler(listOf(KEY_DELETE), {
            KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.ALT_DOWN_MASK)
        })

        // determine capabilities
        capabilities = EnumSet.of(JudoRendererInfo.Capabilities.UTF8)
        terminal.getNumericCapability(InfoCmp.Capability.max_colors)?.let {
            if (it >= 256) {
                capabilities.add(JudoRendererInfo.Capabilities.COLOR_256)
            }
        }
    }

    override fun close() {
        window.clear()

        setCursorType(CursorType.BLOCK)
        terminal.puts(InfoCmp.Capability.exit_ca_mode)
        terminal.puts(InfoCmp.Capability.keypad_local)

        if (enableMouse) {
            terminal.trackMouse(Terminal.MouseTracking.Off)
        }

        terminal.flush()
        terminal.attributes = originalAttributes
        terminal.close()
    }

    override fun appendOutput(line: CharSequence, isPartialLine: Boolean): OutputLine {
        val outputLine = line as? OutputLine ?: OutputLine(line)
        val result = appendOutputLineInternal(outputLine, isPartialLine)
        hadPartialLine = isPartialLine

        if (!isInTransaction) display()
        return result
    }

    override fun replaceLastLine(result: CharSequence) {
        // TODO remove the line completely if empty?

        output[output.lastIndex] = when (result) {
            is OutputLine -> result
            else -> OutputLine(result)
        }
        if (!isInTransaction) display()
    }

    private var cursorType: CursorType = CursorType.BLOCK

    override fun setCursorType(type: CursorType) {
        cursorType = type
    }

    // TODO it'd be great if this could be inline somehow...
    override fun inTransaction(block: () -> Unit) {
        val alreadyInTransaction = isInTransaction
        isInTransaction = true

        block()

        if (!alreadyInTransaction) {
            isInTransaction = false
            display()
        }
    }

    override fun validate() {
        if (terminal is DumbTerminal) {
            throw IllegalArgumentException("Unsupported terminal type ${terminal.name}")
        }
    }

    override fun updateInputLine(line: String, cursor: Int) {
        input = AttributedString.fromAnsi(line)
        this.cursor = cursor
        isCursorOnStatus = false
        if (!isInTransaction) display()
    }

    override fun updateStatusLine(line: String, cursor: Int) {
        status = AttributedString.fromAnsi(line)
        if (cursor >= 0) {
            this.cursor = cursor
            isCursorOnStatus = true
        }
        if (!isInTransaction) display()
    }

    override fun readKey(): KeyStroke {
        while (true) {
            // NOTE: we wait an arbitrary amount of time, because on timeout
            // we just loop again. BUT! We don't use the version without a
            // timeout because if we wait for a *really long time* we occasionally
            // encounter hangs, so hopefully this resolves that.
            val char = terminal.reader().read(30000)
            if (char == NonBlockingReader.READ_EXPIRED) {
                Thread.yield()
                continue
            }

            return when (char) {
                KEY_ESCAPE -> {
                    readEscape()?.let {
                        return it
                    }

                    // not a known escape? ignore and try again
                    return readKey()
                }
                127 -> KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
                '\r'.toInt() -> KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)

                in 1..26 -> {
                    val actualChar = 'a' + char - 1
                    return KeyStroke.getKeyStroke(actualChar, InputEvent.CTRL_DOWN_MASK)
                }

                else -> KeyStroke.getKeyStroke(char.toChar())
            }
        }
    }

    /**
     * Quickly attempt to read either an escape sequence,
     * or a simple <esc> key press
     */
    private fun readEscape(): KeyStroke? {
        val reader = terminal.reader()
        val peek = reader.peek(1)
        escapeSequenceHandlers[peek]?.let { candidates ->
            // looks like an escape sequence
            reader.read() // consume the second char

            val argPeek = reader.read(1)
            candidates[argPeek]?.let {
                return it.invoke()
            }
        }

        return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
    }

    private fun readMouseEvent(): KeyStroke? {
        val event = terminal.readMouseEvent()
        if (event.type == MouseEvent.Type.Wheel) {
            return when(event.button) {
                MouseEvent.Button.WheelUp -> KeyStroke.getKeyStroke(
                    KeyEvent.VK_PAGE_UP,
                    0
                )

                MouseEvent.Button.WheelDown -> KeyStroke.getKeyStroke(
                    KeyEvent.VK_PAGE_DOWN,
                    0
                )

                else -> null
            }
        }

        return null
    }

    private fun registerEscapeHandler(key: InfoCmp.Capability, block: () -> KeyStroke?) {
        val strokes = terminal.keystrokesFor(key) ?: return

        if (strokes.length != 3) throw IllegalStateException("Expected $key to be a 3-char esc sequence")
        if (strokes[0].toInt() != KEY_ESCAPE) throw IllegalStateException("Expected $key to be an esc sequence")

        registerEscapeHandler(strokes.toCharArray().drop(1).map { it.toInt() }, block)
    }

    private fun registerEscapeHandler(strokes: List<Int>, block: () -> KeyStroke?) {
        escapeSequenceHandlers.getOrPut(strokes[0], { HashMap<Int, () -> KeyStroke?>() }).let {
            if (strokes.size == 2) {
                it[strokes[1]] = block
            } else {
                it[KEY_TIMEOUT] = block
            }
        }
    }

    @Synchronized override fun scrollLines(count: Int) {
        // take into account wrapped lines
        val width = windowWidth
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

        var scrolled = 0
        for (i in range) {
            if (i < 0) break
            if (i >= output.size) break

            scrollbackBottom = i
            val displayedLines = output[end - i].getDisplayedLinesCount(width)
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

        if (!isInTransaction) display()
    }

    override fun scrollPages(count: Int) {
        scrollLines(outputWindowHeight * count)
    }

    override fun scrollToBottom() {
        scrollbackBottom = 0
        searchResultLine = -1
        if (!isInTransaction) display()
    }

    /**
     * How many lines we've scrolled back
     */
    override fun getScrollback(): Int = scrollbackBottom

    override fun searchForKeyword(word: CharSequence, direction: Int) {
        val originalSearchResultLine = searchResultLine
        val originalScrollbackBottom = scrollbackBottom
        val originalScrollbackOffset = scrollbackOffset

        if (word != lastSearchKeyword) {
            lastSearchKeyword = word.toString()
            searchResultLine = -1
        }

        do {
            val lastScrollbackBottom = scrollbackBottom
            val lastScrollbackOffset = scrollbackOffset

            val lines = getDisplayLines()
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
        appendOutput("Pattern not found: $word")

    }

    private fun resize() {
        val size = terminal.size
        windowSize.copy(size)
        windowHeight = size.rows
        windowWidth = size.columns
        outputWindowHeight = windowHeight - 2
        window.resize(windowHeight, windowWidth)

        inTransaction {
            window.clear()
            onResized?.invoke()
        }
    }

    private fun handleSignal(signal: Terminal.Signal) {
        try {
            when (signal) {
                Terminal.Signal.WINCH -> resize()
                Terminal.Signal.CONT -> {
                    terminal.enterRawMode()
                    resize()
                }
                else -> {
                    // TODO ?
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Synchronized private fun display() {
        if (outputWindowHeight <= 0) return

        workspace.clear()

        val toOutput = getDisplayLines()
        (toOutput.size..outputWindowHeight).forEach {
            workspace.add(AttributedString.EMPTY)
        }
        workspace.addAll(toOutput)

        val rawStatusCursor =
            if (isCursorOnStatus) cursor
            else 0
        val (statusLine, statusCursor) = fitInputLineToWindow(status, rawStatusCursor)
        workspace.add(statusLine)

        val rawInputCursor =
            if (isCursorOnStatus) 0
            else cursor
        val (inputLine, inputCursor) = fitInputLineToWindow(input, rawInputCursor)
        workspace.add(inputLine)

        val cursorPos: Int
        if (isCursorOnStatus) {
            cursorPos = windowSize.cursorPos(
                windowHeight - 1,
                statusCursor
            )
        } else {
            cursorPos = windowSize.cursorPos(
                windowHeight,
                inputCursor
            )
        }

        // NOTE: jline keeps a reference to the list we provide this
        // method, so we have to make a quick copy. If this becomes an
        // issue, we might be able to "double buffer" and keep an active
        // and a dirty workspace, and swap between them...
        window.resize(windowHeight, windowWidth)
        window.update(workspace.toList(), cursorPos)
        terminal.flush()

        try {
            Curses.tputs(terminal.writer(), cursorType.ansiString)
            terminal.flush()
        } catch (e: NullPointerException) {
            // it's unclear what is causing this NPE (the stack trace
            // seems to indicate it's either the terminal or the cursorType,
            // but neither appears to be possible...
            // Anyway, it only happens from the JLineRenderer constructor,
            // so we can safely ignore it
        }
    }

    // visible for testing
    internal fun fitInputLineToWindow(): Pair<AttributedString, Int> =
        fitInputLineToWindow(input, cursor)

    private fun fitInputLineToWindow(line: AttributedString, cursor: Int): Pair<AttributedString, Int> {
        val maxLineWidth = windowWidth

        if (line.length < maxLineWidth) {
            // convenient shortcut
            return line to cursor
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
        val withIndicator: AttributedString
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

            withIndicator = with(AttributedStringBuilder(maxLineWidth)) {
                if (hasMorePrev) append("…")

                append(windowedInput)

                if (hasMoreNext) append("…")

                toAttributedString()
            }
        }

        return withIndicator.toAttributedString() to (absolutePageCursor + cursorOffset)
    }

    fun getDisplayLines(): List<AttributedString> {
        val start = output.lastIndex - scrollbackBottom
        val end = maxOf(0, start - outputWindowHeight)
        val lines = output.slice(end..start)
            .asSequence()
            .flatMap { it.getDisplayLines(windowWidth).asSequence() }
            .toList()
            .dropLast(scrollbackOffset)
            .takeLast(outputWindowHeight)

        if (searchResultLine >= 0) {
            val lineIndex = searchResultLine
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

                val mutableLines = lines.toMutableList()
                mutableLines[lineIndex] = newString
                return mutableLines
            }
        }

        return lines
    }

    /**
     * @return The actual [OutputLine] that was put into the buffer
     */
    private fun appendOutputLineInternal(line: OutputLine, isPartialLine: Boolean): OutputLine {
        val splitCandidate: OutputLine
        val linesMod: Int
        if (hadPartialLine) {
            hadPartialLine = false

            // we're adding one less than expected, because at least
            // one part of OutputLine is replacing something
            linesMod = -1

            // merge partial line
            val original = output.removeLast()
            original.append(line)

            splitCandidate = original
        } else {
            // new line
            if (output.isNotEmpty()) {
                val previous = output.last()
                line.setStyleHint(previous.getFinalStyle())
            }
            splitCandidate = line
            linesMod = 0 // no change
        }

        val linesAdded: Int
        if (isPartialLine) {
            // never split partial lines right away
            output.add(splitCandidate)
            linesAdded = 1
        } else {
            // full line. split away!
            val split = splitCandidate.getDisplayOutputLines(windowWidth)
            linesAdded = split.size
            split.forEach(output::add)
        }

        val atBottom = scrollbackBottom == 0 && scrollbackOffset == 0
        if (!atBottom) {
            val change = linesAdded + linesMod
            scrollbackBottom += change
        }

        return splitCandidate
    }
}

fun Terminal.keystrokesFor(capability: InfoCmp.Capability): String? {
    try {
        val str = getStringCapability(capability)
        if (str != null) {
            val sw = StringWriter()
            Curses.tputs(sw, str)
            return sw.toString()
        }
    } catch (e: IOException) {
        // Ignore
    }

    return null
}
