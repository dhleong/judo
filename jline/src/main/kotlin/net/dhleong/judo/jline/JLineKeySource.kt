package net.dhleong.judo.jline

import net.dhleong.judo.BlockingKeySource
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.Modifier
import org.jline.terminal.MouseEvent
import org.jline.terminal.Terminal
import org.jline.utils.Curses
import org.jline.utils.InfoCmp
import org.jline.utils.NonBlockingReader
import java.io.IOException
import java.io.StringWriter

/**
 * @author dhleong
 */
class JLineKeySource(
    private val terminal: Terminal
) : BlockingKeySource {

    private var lastKeyTime: Long = -1
    private val escapeSequenceHandlers = HashMap<Int, HashMap<Int, () -> Key?>>(8)

    init {
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
                        // since having multiple lines makes sense in a multi-line editor),
                        // when the last-read keyStroke was super recent, we just treat it
                        // as an ENTER in order to avoid accidentally triggering a ctrl-j mapping
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
