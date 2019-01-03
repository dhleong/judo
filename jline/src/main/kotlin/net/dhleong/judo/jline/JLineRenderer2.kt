package net.dhleong.judo.jline

import net.dhleong.judo.*
import net.dhleong.judo.input.Key
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoTabpage2
import org.jline.terminal.*
import org.jline.terminal.impl.DumbTerminal
import org.jline.utils.Curses
import org.jline.utils.Display
import org.jline.utils.InfoCmp
import java.io.IOException
import java.io.StringWriter
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

// ascii codes:
const val KEY_ESCAPE = 27
const val KEY_DELETE = 127

// this is returned by read() when it times out
const val KEY_TIMEOUT = -2

internal const val ELLIPSIS = "…"

/**
 * @author dhleong
 */
class JLineRenderer2(
    override var settings: StateMap,
    private val enableMouse: Boolean = false
) : JudoRenderer2 {

    private val terminal = TerminalBuilder.terminal()!!
    private val window = Display(terminal, true)
    private val originalAttributes: Attributes

    private val windowSize = Size(0, 0)

    override val terminalType: String
        get() = terminal.type

    override val capabilities: EnumSet<JudoRendererInfo.Capabilities>

    override val windowWidth: Int
        get() = terminal.width
    override val windowHeight: Int
        get() = terminal.height

    override var onResized: OnResizedEvent? = null

    override var currentTabpage: IJudoTabpage2?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    private var isLoading: Boolean = true
    private val input = InputLine()
    private val inputHelper = TerminalInputLineHelper(settings)
    private val renderedInput = mutableListOf<FlavorableCharSequence>()
    private var cursorType: CursorType = CursorType.BLOCK

    private val escapeSequenceHandlers = HashMap<Int, HashMap<Int, () -> Key?>>(8)
    private val transactionDepth = AtomicInteger(0)

    init {
        terminal.handle(Terminal.Signal.WINCH, this::handleSignal)
        terminal.handle(Terminal.Signal.CONT, this::handleSignal)

        terminal.enterRawMode()

        originalAttributes = terminal.attributes
        val newAttr = Attributes(originalAttributes)
        newAttr.setLocalFlags(EnumSet.of(Attributes.LocalFlag.ICANON, Attributes.LocalFlag.ECHO, Attributes.LocalFlag.IEXTEN), false)
        newAttr.setInputFlags(EnumSet.of(Attributes.InputFlag.IXON, Attributes.InputFlag.ICRNL, Attributes.InputFlag.INLCR), false)
        newAttr.setControlChar(Attributes.ControlChar.VMIN, 1)
        newAttr.setControlChar(Attributes.ControlChar.VTIME, 0)
        newAttr.setControlChar(Attributes.ControlChar.VINTR, 0)
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
        registerEscapeHandler(listOf(KEY_DELETE)) {
            Key.parse("alt bs")
        }

        // determine capabilities
        capabilities = EnumSet.of(JudoRendererInfo.Capabilities.UTF8)
        terminal.getNumericCapability(InfoCmp.Capability.max_colors)?.let {
            if (it >= 256) {
                capabilities.add(JudoRendererInfo.Capabilities.COLOR_256)
            }
        }
    }

    override fun validate() {
        if (terminal is DumbTerminal) {
            throw IllegalArgumentException("Unsupported terminal type ${terminal.name}")
        }
    }

    override fun setCursorType(type: CursorType) {
        inTransaction {
            cursorType = type
        }
    }

    override fun updateInputLine(line: String, cursor: Int) {
        inTransaction {
            input.line = FlavorableStringBuilder.withDefaultFlavor(line)
            input.cursorIndex = cursor
        }
    }

    override fun redraw() {
        inTransaction {
            render()
        }
    }

    override fun setLoading(isLoading: Boolean) {
        if (this.isLoading != isLoading) {
            this.isLoading = isLoading
            render()
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

    override fun beginUpdate() {
        transactionDepth.incrementAndGet()
    }

    override fun finishUpdate() {
        val depth = transactionDepth.decrementAndGet()
        if (depth == 0) {
            render()
        } else if (depth < 0) {
            throw IllegalStateException("Unbalanced call to finishUpdate")
        }
    }

    private fun render() {
        val display = JLineDisplay(windowWidth, windowHeight) // FIXME reuse?

        val cursor = if (isLoading) {
            display.withLine(0, 0) {
                append("Loading...")
            }
            windowSize.cursorPos(windowHeight - 1, 0)
        } else {
            render(display)
        }

        window.update(display.toAttributedStrings(), cursor)
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

    /** @return the cursor */
    internal fun render(display: JLineDisplay): Int {
        val tabpage = currentTabpage as? JLineTabpage
            ?: throw IllegalStateException("Current tabpage is not JLine: $currentTabpage")

        tabpage.render(display)

        val win = tabpage.currentWindow
        val isCursorOnStatus = win.isFocusable && win.isFocused && win.statusCursor != -1
        val rawInputCursor = when {
            isCursorOnStatus -> 0
            tabpage.currentWindow.isFocusable -> input.cursorIndex
            else -> 0
        }

        renderedInput.clear()
        inputHelper.fitInputLinesToWindow(input, renderedInput)

        val windowY = tabpage.getYPositionOf(win)
        val windowX = 0 // TODO when we add vsplit support

        // FIXME: the input line(s) render differently...
        return windowSize.cursorPos(
            windowX + win.statusCursor,
            windowY + win.height - 1
        )
    }

    private fun resize() {
        val size = terminal.size
        windowSize.copy(size)

        val windowHeight = size.rows
        val windowWidth = size.columns
        window.resize(windowHeight, windowWidth)

        updateSize()
    }

    internal fun updateSize() {
        currentTabpage?.resize(windowWidth, windowHeight - 1)
        window.clear()
        onResized?.invoke()
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
        escapeSequenceHandlers.getOrPut(strokes[0]) { HashMap() }.let {
            if (strokes.size == 2) {
                it[strokes[1]] = block
            } else {
                it[KEY_TIMEOUT] = block
            }
        }
    }
}

private fun Terminal.keystrokesFor(capability: InfoCmp.Capability): String? {
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
