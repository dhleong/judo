package net.dhleong.judo

import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.impl.DumbTerminal
import org.jline.utils.Display
import org.jline.utils.InfoCmp
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.IOException
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
    private var scrollbackTop = 0

    private var input = ""
    private var status = ""
    private var cursor = 0
    private var isCursorOnStatus = false

    private val workspace = mutableListOf<String>()

    init {
        terminal.handle(Terminal.Signal.WINCH, this::handleSignal)
        terminal.enterRawMode()

        terminal.puts(InfoCmp.Capability.enter_ca_mode)
        terminal.puts(InfoCmp.Capability.keypad_xmit)
        terminal.writer().flush()

        resize()
    }

    override fun close() {
        window.clear()
        terminal.puts(InfoCmp.Capability.exit_ca_mode)
        terminal.puts(InfoCmp.Capability.key_exit)
        terminal.writer().flush()
        terminal.close()
    }

    override fun appendOutput(buffer: CharArray, count: Int) {
        var lastLineEnd = 0
        @Suppress("LoopToCallChain") // actually it seems we need the loop here
        for (i in 0 until count) {
            if (i >= lastLineEnd) {
                when (buffer[i]) {
                    '\n' -> {
                        appendOutputLine(buffer.substring(lastLineEnd, i))
                        if (i + 1 < count && buffer[i + 1] == '\r') {
                            lastLineEnd = i + 2
                        } else {
                            lastLineEnd = i + 1
                        }
                    }
                    '\r' -> {
                        appendOutputLine(buffer.substring(lastLineEnd, i))
                        lastLineEnd = i + 2
                    }
                }
            }
        }

        if (lastLineEnd < count) {
            appendOutputLine(buffer.substring(lastLineEnd, count))
        }
    }

    override fun appendOutputLine(line: String) {
        val atBottom = scrollbackTop + outputWindowHeight == output.size
        output.add(line)

        if (atBottom && output.size > outputWindowHeight) {
            ++scrollbackTop
        }

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
        val char = terminal.input().read()
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

    fun getOutputLines(): List<String> = output.toList()

    fun getScrollbackTop(): Int = scrollbackTop

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
            output.drop(scrollbackTop)
                .take(outputWindowHeight)
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
}

private fun CharArray.substring(start: Int, end: Int) =
    String(this, start, end - start)
