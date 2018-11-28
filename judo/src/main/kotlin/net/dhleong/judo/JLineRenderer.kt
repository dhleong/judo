package net.dhleong.judo

import net.dhleong.judo.input.Key
import net.dhleong.judo.input.Modifier
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.OutputLine
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
import java.io.IOException
import java.io.StringWriter
import java.util.EnumSet

// ascii codes:
val KEY_ESCAPE = 27
val KEY_DELETE = 127

// this is returned by read() when it times out
val KEY_TIMEOUT = -2

internal val ELLIPSIS = "…"

/**
 * @author dhleong
 */
class JLineRenderer(
    override var settings: StateMap,
    private val enableMouse: Boolean = false
) : JudoRenderer, BlockingKeySource  {

    override val terminalType: String
        get() = terminal.type
    override val capabilities: EnumSet<JudoRendererInfo.Capabilities>

    override var onResized: OnResizedEvent? = null

    override var currentTabpage: IJudoTabpage? = null

    private val terminal = TerminalBuilder.terminal()!!
    private val window = Display(terminal, true)
    private val originalAttributes: Attributes

    override var windowHeight = -1
    override var windowWidth = -1
    private val windowSize = Size(0, 0)

    private var input = AttributedString.EMPTY
    private var cursor = 0
    private var lastKeyTime: Long = -1

    private val escapeSequenceHandlers = HashMap<Int, HashMap<Int, () -> Key?>>(8)

    private var cursorType: CursorType = CursorType.BLOCK

    private var isInTransaction = false
    private val outputWorkspace = ArrayList<CharSequence>(64)
    private var lastInputLinesCount: Int = 1

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
            Key.parse("shift tab")
        }
        registerEscapeHandler(InfoCmp.Capability.key_down) {
            Key.parse("down")
        }
        registerEscapeHandler(InfoCmp.Capability.key_up) {
            Key.parse("up")
        }
        registerEscapeHandler(listOf(KEY_DELETE), {
            Key.parse("alt bs")
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

    private fun resize() {
        val size = terminal.size
        windowSize.copy(size)
        windowHeight = size.rows
        windowWidth = size.columns
        window.resize(windowHeight, windowWidth)

        updateSize()
    }

    internal fun updateSize() {
        doInTransaction {
            currentTabpage?.resize(windowWidth, windowHeight - 1)
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

    override fun setCursorType(type: CursorType) {
        doInTransaction {
            cursorType = type
        }
    }

    override fun validate() {
        if (terminal is DumbTerminal) {
            throw IllegalArgumentException("Unsupported terminal type ${terminal.name}")
        }
    }

    override fun updateInputLine(line: String, cursor: Int) {
        doInTransaction {
            input = AttributedString.fromAnsi(line)
            this.cursor = cursor
        }
    }

    override fun readKey(): Key? {
        // NOTE: we wait an arbitrary amount of time, because on timeout
        // we just loop again. BUT! We don't use the version without a
        // timeout because if we wait for a *really long time* we occasionally
        // encounter hangs, so hopefully this resolves that.
        val char = terminal.reader().read(300)
        if (char == NonBlockingReader.READ_EXPIRED) {
            return null
        }

        val lastKey = lastKeyTime
        lastKeyTime = System.currentTimeMillis()

        return when (char) {
            KEY_ESCAPE -> {
                readEscape()?.let {
                    return it
                }

                // not a known escape? ignore and try again
                return null
            }
            127 -> Key.BACKSPACE
            '\r'.toInt() -> Key.ENTER

            in 1..26 -> {
                if (char == 10) {
                    val delta = System.currentTimeMillis() - lastKey
                    if (delta < 7L) {  // ignore the magic number, just see below:
                        // NOTE: 10 means ctrl-j, but also means linefeed, especially
                        // when pasting text. As a bit of a hack (even vim doesn't do this,
                        // since with multiple lines makes since in a multi-line editor), when
                        // the last-read keyStroke was super recent, we just treat it as an
                        // ENTER in order to avoid accidentally triggering a ctrl-j mapping
                        return Key.ENTER
                    }
                }

                val actualChar = 'a' + char - 1
                return Key.ofChar(actualChar, Modifier.CTRL)
            }

            else -> Key.ofChar(char.toChar())
        }

    }

    /**
     * Quickly attempt to read either an escape sequence,
     * or a simple <esc> key press
     */
    private fun readEscape(): Key? {
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

        return Key.ESCAPE
    }

    private fun readMouseEvent(): Key? {
        val event = terminal.readMouseEvent()
        if (event.type == MouseEvent.Type.Wheel) {
            return when(event.button) {
                MouseEvent.Button.WheelUp -> Key.parse("PageUp")
                MouseEvent.Button.WheelDown -> Key.parse("PageDown")

                else -> null
            }
        }

        return null
    }

    private fun registerEscapeHandler(key: InfoCmp.Capability, block: () -> Key?) {
        val strokes = terminal.keystrokesFor(key) ?: return

        if (strokes.length != 3) throw IllegalStateException("Expected $key to be a 3-char esc sequence")
        if (strokes[0].toInt() != KEY_ESCAPE) throw IllegalStateException("Expected $key to be an esc sequence")

        registerEscapeHandler(strokes.toCharArray().drop(1).map { it.toInt() }, block)
    }

    private fun registerEscapeHandler(strokes: List<Int>, block: () -> Key?) {
        escapeSequenceHandlers.getOrPut(strokes[0], { HashMap<Int, () -> Key?>() }).let {
            if (strokes.size == 2) {
                it[strokes[1]] = block
            } else {
                it[KEY_TIMEOUT] = block
            }
        }
    }

    // TODO it'd be great if this could be inline somehow...
    override fun inTransaction(block: () -> Unit) {
        doInTransaction(block)
    }

    internal inline fun doInTransaction(block: () -> Unit) {
        val alreadyInTransaction = isInTransaction
        isInTransaction = true

        try {
            block()
        } finally {
            if (!alreadyInTransaction) {
                isInTransaction = false
                redraw()
            }
        }
    }

    private var renderedSplashScreen = false

    @Synchronized override fun redraw() {
        val lastInputLineCount = this.lastInputLinesCount
        val (lines, cursorRow, cursorCol) = getDisplayLines()
        if (lines.isEmpty()) {
            // splash screen?
            if (windowHeight > 0) {
                renderedSplashScreen = true
                window.resize(windowWidth, windowHeight)
                window.update(
                    listOf(AttributedString.fromAnsi("Loading Judo...")),
                    windowSize.cursorPos(windowHeight - 1, 0)
                )
                terminal.flush()
            }

            // either way, don't do anything else
            return
        }

        // NOTE: for whatever reason, when we the number of input lines goes down,
        // the rendering gets messed up if we don't reset first; increasing the count
        // seems to work just fine.
        if (renderedSplashScreen || lastInputLineCount > this.lastInputLinesCount) {
            renderedSplashScreen = false
            window.clear()
            window.reset()
        }

        val cursorPos = windowSize.cursorPos(
            cursorRow,
            cursorCol
        )

        if (lines.size != windowHeight) {
            throw IllegalStateException("Expected $windowHeight lines but got ${lines.size}")
        }

        window.resize(windowHeight, windowWidth)
        window.update(lines, cursorPos)
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

    fun getDisplayLines(): Triple<List<AttributedString>, Int, Int> {
        val tabpage = currentTabpage ?: return Triple(emptyList<AttributedString>(), 0, 0)
        val window = tabpage.currentWindow

        outputWorkspace.clear()
        tabpage.getDisplayLines(outputWorkspace)

        if (outputWorkspace.size != windowHeight - 1) {
            throw IllegalStateException(
                "$tabpage generated ${outputWorkspace.size} lines, " +
                "but expected ${windowHeight - 1} (height = ${tabpage.height})"
            )
        }

        val isCursorOnStatus = window.isFocusable && window.isFocused && window.statusCursor != -1
        val rawInputCursor = when {
            isCursorOnStatus -> 0
            tabpage.currentWindow.isFocusable -> cursor
            else -> 0
        }
        val (inputLines, inputCursor) = fitInputLinesToWindow(input, rawInputCursor)
        outputWorkspace.addAll(inputLines)
        lastInputLinesCount = inputLines.size

        // trim off extra lines at the beginning
        // This is not super efficient, but it shouldn't happen often enough to be a problem
        for (r in 1..(inputLines.size - 1)) {
            outputWorkspace.removeAt(0)
        }

        val rawStatusCursor = when {
            isCursorOnStatus -> window.statusCursor
            else -> 0
        }

        val cursorRow: Int
        val cursorCol: Int
        if (isCursorOnStatus) {
            val windowY = tabpage.getYPositionOf(window)
            val windowBottom = window.height + windowY
            cursorRow = windowBottom - inputLines.size
            cursorCol = rawStatusCursor
        } else {
            val (inputCursorRow, inputCursorCol) = inputCursor
            cursorRow = windowHeight - inputLines.size + inputCursorRow
            cursorCol = inputCursorCol
        }

        // NOTE: JLine uses our list as-is, so if we modify it
        // after the fact, things get weiiiird. We could maybe
        // double-buffer it to avoid constantly allocating new arrays...
        return Triple(outputWorkspace.map { it as AttributedString }, cursorRow, cursorCol)
    }

    // visible for testing
    internal fun fitInputLinesToWindow(): Pair<List<AttributedString>, Pair<Int, Int>> =
        fitInputLinesToWindow(input, cursor)

    private fun fitInputLinesToWindow(line: AttributedString, cursor: Int): Pair<List<AttributedString>, Pair<Int, Int>> {
        val maxLineWidth = windowWidth
        val maxLines = settings[MAX_INPUT_LINES]

        if (line.length < maxLineWidth || maxLines == 1) {
            // convenient shortcut
            val (scrolledLine, cursorCol) = fitInputLineToWindow(line, cursor)
            return listOf(scrolledLine) to (0 to cursorCol)
        }

        val output = OutputLine(line)
        var lines = output.getDisplayLines(maxLineWidth, settings[WORD_WRAP])
        var fitCursor = fitCursorInLines(lines, cursor)

        if (fitCursor.second == maxLineWidth) {
            fitCursor = fitCursor.first + 1 to 0
            lines = lines.toMutableList().apply { add(AttributedString.EMPTY) }
        }

        if (lines.size <= maxLines) {
            // easy case
            return lines to fitCursor
        }

        // limit number of lines
        val (cursorRow, cursorCol) = fitCursor
        var start = maxOf(cursorRow - maxLines / 2, 0)
        val end = minOf(start + maxLines, lines.size) - 1
        val result = ArrayList<AttributedString>(maxLines)

        if (end == lines.lastIndex) {
            // NOTE: end is inclusive, so we don't want end - start = maxLines
            while (end - start < (maxLines - 1) && start > 0) {
                --start
            }
        }

        val first = lines[start]
        if (start == 0) {
            result.add(first)
        } else {
            result.add(
                with(AttributedStringBuilder(first.length)) {
                    appendAnsi(ELLIPSIS)
                    append(first, 1, first.length)

                    toAttributedString()
                }
            )
        }

        // no extra allocations, please
        @Suppress("LoopToCallChain")
        for (i in (start + 1)..end - 1) {
            result.add(lines[i])
        }

        val last = lines[end]
        if (end == lines.lastIndex) {
            result.add(last)
        } else {
            result.add(
                with(AttributedStringBuilder(last.length)) {
                    append(last, 0, last.length - 1)
                    appendAnsi(ELLIPSIS)

                    toAttributedString()
                }
            )
        }

        return result to ((cursorRow - start) to cursorCol)
    }

    private fun fitCursorInLines(lines: List<AttributedString>, cursor: Int): Pair<Int, Int> {
        // thanks to word-wrap, cursor row/col is not an exact fit, so just go find it
        var cursorCol = cursor

        val lastLine = lines.lastIndex
        for (row in lines.indices) {
            val rowLen = lines[row].length
            if (cursorCol < rowLen || (row == lastLine && cursorCol == rowLen)) {
                return row to cursorCol
            }

            cursorCol -= rowLen
        }

        throw IllegalStateException("Couldn't fit cursor $cursor in $lines")
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
                if (hasMorePrev) append(ELLIPSIS)

                append(windowedInput)

                if (hasMoreNext) append(ELLIPSIS)

                toAttributedString()
            }
        }

        return withIndicator.toAttributedString() to (absolutePageCursor + cursorOffset)
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
