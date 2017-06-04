package net.dhleong.judo

import org.jline.terminal.Attributes
import org.jline.terminal.Attributes.*
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.impl.DumbTerminal
import org.jline.utils.AttributedCharSequence
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.Display
import org.jline.utils.InfoCmp
import java.awt.SystemColor.window
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.IOException
import java.util.EnumSet
import javax.swing.KeyStroke


/**
 * @author dhleong
 */

class JLineRenderer : JudoRenderer, BlockingKeySource {
    override val terminalType: String
        get() = terminal.type

    private val terminal = TerminalBuilder.terminal()!!
    private val window = Display(terminal, true)

    override var windowHeight = -1
    override var windowWidth = -1
    private var outputWindowHeight = -1
    private val windowSize = Size(0, 0)

    // TODO circular buffer with max size
    private val output = mutableListOf<AttributedString>()
    private var scrollbackBottom = 0

    private var hadPartialLine = false

    private var input = AttributedString.EMPTY
    private var status = AttributedString.EMPTY
    private var cursor = 0
    private var isCursorOnStatus = false

    private val workspace = mutableListOf<AttributedString>()
    private val originalAttributes: Attributes

    private var isInTransaction = false

    init {
        terminal.handle(Terminal.Signal.WINCH, this::handleSignal)
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
        terminal.flush()

        resize()
    }

    override fun close() {
        window.clear()

        terminal.puts(InfoCmp.Capability.exit_ca_mode)
        terminal.puts(InfoCmp.Capability.keypad_local)
        terminal.flush()
        terminal.attributes = originalAttributes
        terminal.close()
    }

    override fun appendOutput(line: CharSequence, isPartialLine: Boolean) {
        appendOutputLineInternal(line)
        hadPartialLine = isPartialLine

        if (!isInTransaction) display()
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
        val char = terminal.reader().read()
        return when (char) {
            27 -> readEscape()
            127 -> KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
            '\r'.toInt() -> KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)

            in 1..26 -> {
                val actualChar = 'a' + char - 1
                return KeyStroke.getKeyStroke(actualChar, InputEvent.CTRL_DOWN_MASK)
            }

            else -> KeyStroke.getKeyStroke(char.toChar())
        }
    }

    /**
     * Quickly attempt to read either an escape sequence,
     * or a simple <esc> key press
     */
    private fun readEscape(): KeyStroke {
        val reader = terminal.reader()
        val peek = reader.peek(1)
        if (peek == 91) { // 91 == [
            // looks like an escape sequence
            reader.read() // consume [

            val argPeek = reader.read(1)
            when (argPeek) {
                90 -> return KeyStroke.getKeyStroke(
                    KeyEvent.VK_TAB,
                    KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK
                )
            }
        }

        return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
    }

    override fun scrollLines(count: Int) {
        scrollbackBottom = maxOf(
            0,
            minOf(
                output.size - outputWindowHeight,
                scrollbackBottom + count)
        )
        if (!isInTransaction) display()
    }

    override fun scrollPages(count: Int) {
        scrollLines(outputWindowHeight * count)
    }

    override fun scrollToBottom() {
        scrollbackBottom = 0
        if (!isInTransaction) display()
    }

    fun getOutputLines(): List<String> = output.map { it.toAnsi() }.toList()

    /**
     * How many lines we've scrolled back
     */
    override fun getScrollback(): Int = scrollbackBottom

    private fun resize() {
        val size = terminal.size
        windowSize.copy(size)
        windowHeight = size.rows
        windowWidth = size.columns
        outputWindowHeight = windowHeight - 2
        window.resize(windowHeight, windowWidth)
        if (!isInTransaction) display()
    }

    private fun handleSignal(signal: Terminal.Signal) {
        try {
            when (signal) {
                Terminal.Signal.WINCH -> resize()
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

        workspace.add(status)
        workspace.add(input)

        val cursorPos = windowSize.cursorPos(
            windowHeight - (if (isCursorOnStatus) 1 else 0),
            cursor
        )

        // NOTE: jline keeps a reference to the list we provide this
        // method, so we have to make a quick copy. If this becomes an
        // issue, we might be able to "double buffer" and keep an active
        // and a dirty workspace, and swap between them...
        window.resize(windowHeight, windowWidth)
        window.update(workspace.toList(), cursorPos)
        terminal.flush()
    }

    fun getDisplayLines(): List<AttributedString> {
        val start = maxOf(0, output.size - scrollbackBottom - outputWindowHeight)
        val end = minOf(output.size - 1, (start + outputWindowHeight - 1))
        return output.slice(start..end)
    }

    /**
     * Wrapper for [appendOutputLineInternal] that hard-wraps
     * lines based on current window width
     */
    private fun appendOutputLineInternal(line: CharSequence) {
        val builder = AttributedStringBuilder(line.length)
        builder.tabs(0)
        if (line is AttributedCharSequence) {
            builder.append(line.toAttributedString())
        } else {
            builder.appendAnsi(line.toString())
        }

        if (builder.columnLength() > windowWidth) {

            builder.columnSplitLength(windowWidth)
                .forEach { appendOutputAttributedLine(it) }
//            for (i in 0 until line.length - windowWidth step windowWidth) {
//                appendOutputAttributedLine(builder.columnSubSequence(i, i + windowWidth))
//            }
        } else {
            appendOutputAttributedLine(builder.toAttributedString())
        }
    }

    /**
     * @param line MUST be guaranteed to fit on a single line
     */
    private fun appendOutputAttributedLine(line: AttributedString) {
        if (hadPartialLine) {
            hadPartialLine = false

            val end = output.size - 1
            val original = output[end]
            val builder = AttributedStringBuilder(original.length + line.length)
            builder.append(original)
            builder.append(line)
            output[end] = builder.toAttributedString()
        } else {
            val atBottom = scrollbackBottom == 0
            output.add(line)

            if (!atBottom && output.size > outputWindowHeight) {
                ++scrollbackBottom
            }
        }
    }
}

private fun ansiToAttributed(line: CharSequence): AttributedString =
    if (line is AttributedCharSequence) line.toAttributedString()
    else AttributedString.fromAnsi(line.toString())

