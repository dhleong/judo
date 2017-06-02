package net.dhleong.judo

import org.jline.terminal.Attributes
import org.jline.terminal.Attributes.*
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.impl.DumbTerminal
import org.jline.utils.Display
import org.jline.utils.InfoCmp
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
    private val output = mutableListOf<String>()
    private var scrollbackBottom = 0

    private var hadPartialLine = false

    private var input = ""
    private var status = ""
    private var cursor = 0
    private var isCursorOnStatus = false

    private val workspace = mutableListOf<String>()
    private val originalAttributes: Attributes

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

    override fun appendOutput(buffer: CharArray, count: Int) {
        var lastLineEnd = 0
        @Suppress("LoopToCallChain") // actually it seems we need the loop here
        for (i in 0 until count) {
            if (i >= lastLineEnd) {
                when (buffer[i]) {
                    '\n' -> {
                        appendOutputLineInternal(buffer.substring(lastLineEnd, i))
                        if (i + 1 < count && buffer[i + 1] == '\r') {
                            lastLineEnd = i + 2
                        } else {
                            lastLineEnd = i + 1
                        }
                    }
                    '\r' -> {
                        appendOutputLineInternal(buffer.substring(lastLineEnd, i))
                        if (i + 1 < count && buffer[i + 1] == '\n') {
                            lastLineEnd = i + 2
                        } else {
                            lastLineEnd = i + 1
                        }
                    }
                }
            }
        }

        if (lastLineEnd < count) {
            appendOutputLineInternal(buffer.substring(lastLineEnd, count))
            hadPartialLine = true
        } else {
            // maybe we did some echo() or something before the rest
            // came in? Either way, clear the flag
            hadPartialLine = false
        }

        display()
    }

    override fun appendOutputLine(line: String) {
        appendOutputLineInternal(line)

        display()
    }


    override fun validate() {
        if (terminal is DumbTerminal) {
            throw IllegalArgumentException("Unsupported terminal type ${terminal.name}")
        }
    }

    override fun updateInputLine(line: String, cursor: Int) {
        input = line
        this.cursor = cursor
        isCursorOnStatus = false
        display()
    }

    override fun updateStatusLine(line: String, cursor: Int) {
        status = line
        if (cursor >= 0) {
            this.cursor = cursor
            isCursorOnStatus = true
        }
        display()
    }

    override fun readKey(): KeyStroke {
        val char = terminal.reader().read()
        return when (char) {
            27 -> KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
            127 -> KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
            '\r'.toInt() -> KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)

            in 1..26 -> {
                val actualChar = 'a' + char - 1
                return KeyStroke.getKeyStroke(actualChar, InputEvent.CTRL_DOWN_MASK)
            }

            else -> KeyStroke.getKeyStroke(char.toChar())
        }
    }

    override fun scrollLines(count: Int) {
        scrollbackBottom = maxOf(
            0,
            minOf(
                output.size - outputWindowHeight,
                scrollbackBottom + count)
        )
        display()
    }

    override fun scrollPages(count: Int) {
        scrollLines(outputWindowHeight * count)
    }

    override fun scrollToBottom() {
        scrollbackBottom = 0
        display()
    }

    fun getOutputLines(): List<String> = output.toList()

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
        display()
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

    private fun display() {
        if (outputWindowHeight <= 0) return

        workspace.clear()

        workspace.addAll(
            output.dropLast(scrollbackBottom)
                .takeLast(outputWindowHeight)
        )

        (workspace.size..outputWindowHeight).forEach {
            workspace.add("")
        }

        workspace.add(status)
        workspace.add(input)

        val cursorPos = windowSize.cursorPos(
            windowHeight - (if (isCursorOnStatus) 1 else 0),
            cursor
        )

        window.resize(windowHeight, windowWidth)
        window.updateAnsi(workspace, cursorPos)
        terminal.flush()
    }

    /**
     * Wrapper for [appendOutputLineInternal] that hard-wraps
     * lines based on current window width
     */
    private fun appendOutputLineInternal(line: String) {
        // TODO
//        if (line.length > windowWidth) {
//            for (i in 0 until line.length - windowWidth step windowWidth) {
//                appendOutputLineSingle(line.substring(i, i + windowWidth))
//            }
//        } else {
//        }
        appendOutputLineSingle(line)
    }

    /**
     * @param line MUST be guaranteed to fit on a single line
     */
    private fun appendOutputLineSingle(line: String) {
        if (hadPartialLine) {
            hadPartialLine = false

            val end = output.size - 1
            output[end] += line
        } else {
            val atBottom = scrollbackBottom == 0
            output.add(line)

            if (!atBottom && output.size > outputWindowHeight) {
                ++scrollbackBottom
            }
        }
    }
}

private fun CharArray.substring(start: Int, end: Int) =
    String(this, start, end - start)
