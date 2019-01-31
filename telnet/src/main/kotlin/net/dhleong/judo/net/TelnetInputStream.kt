package net.dhleong.judo.net

import java.io.BufferedInputStream
import java.util.concurrent.atomic.AtomicBoolean

private enum class State {
    IDLE,
    IAC,

    WILL,
    WONT,
    DO,
    DONT,

    SB,
    IAC_IN_SB
}

private const val EOF = -1
private const val NO_DATA = -2

/**
 * @author dhleong
 */
internal class TelnetInputStream(
    delegate: InputStream,
    maxSubnegotiationSize: Int = 8192,
    private val onTelnetEvent: (TelnetEvent) -> Unit
) : InputStream() {

    private val eventBuffer = ByteArray(maxSubnegotiationSize)
    private var eventBufferSize: Int = 0
    private val delegate = BufferedInputStream(delegate)

    private var state = State.IDLE
    private val hasReadEof = AtomicBoolean(false)

    override fun read(): Int {
        var byte: Int
        do {
            byte = readNonBlocking()
        } while (byte == NO_DATA)
        return byte
    }

    override fun read(b: ByteArray): Int = read(b, 0, b.size)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (hasReadEof.get()) {
            return EOF
        }

        var read = 0
        for (i in off until off + len) {
            // block to get at least 1 byte
            var byte: Int
            do {
                byte = readNonBlocking(readAtLeastOnce = read == 0)
            } while (read == 0 && byte == NO_DATA)

            if (byte == EOF && read > 0) {
                // reached EOF, but we still have bytes to deliver
                break
            } else if (byte == EOF) {
                return EOF
            } else if (byte == NO_DATA && read > 0) {
                // not eof, but no data available;
                break
            }

            b[i] = (byte and 0xff).toByte()
            ++read
        }

        return read
    }

    override fun available(): Int = delegate.available()

    private fun readNonBlocking(readAtLeastOnce: Boolean = true): Int {
        var first = true
        var read: Int
        do {
            if (!readAtLeastOnce || !first) {
                // make sure we won't block
                if (delegate.available() <= 0) {
                    return NO_DATA
                }
            }

            read = delegate.read()
            first = false
        } while (read != EOF && handleTelnetCommand((read and 0xff).toByte()))

        if (read == EOF) {
            hasReadEof.set(true)
        }

        return read
    }

    private fun handleTelnetCommand(byte: Byte): Boolean = when (state) {
        State.IDLE -> when (byte) {
            TELNET_IAC -> consumeAnd(State.IAC)
            else -> false // return the byte
        }

        State.IAC -> when (byte) {
            TELNET_IAC -> returnByteAnd(State.IDLE)  // literal IAC

            TELNET_DO -> consumeAnd(State.DO)
            TELNET_DONT -> consumeAnd(State.DONT)
            TELNET_WILL -> consumeAnd(State.WILL)
            TELNET_WONT -> consumeAnd(State.WONT)
            TELNET_SB -> consumeAnd(State.SB) {
                eventBuffer[0] = byte
                eventBufferSize = 1
            }
            TELNET_SE -> consumeAnd(State.IDLE) // unexpected byte! ignore it

            else -> consumeAnd(State.IDLE) {
                // simple command
                onTelnetEvent(byte)
            }
        }

        State.DO -> consumeAnd(State.IDLE) { onTelnetEvent(TELNET_DO, byte) }
        State.DONT -> consumeAnd(State.IDLE) { onTelnetEvent(TELNET_DONT, byte) }
        State.WILL -> consumeAnd(State.IDLE) { onTelnetEvent(TELNET_WILL, byte) }
        State.WONT -> consumeAnd(State.IDLE) { onTelnetEvent(TELNET_WONT, byte) }

        State.SB -> when (byte) {
            TELNET_IAC -> consumeAnd(State.IAC_IN_SB)
            else -> consumeAnd(State.SB) {
                // TODO grow eventBuffer if needed
                eventBuffer[eventBufferSize++] = byte
            }
        }

        State.IAC_IN_SB -> when (byte) {
            TELNET_IAC -> consumeAnd(State.SB) {
                // literal IAC
                eventBuffer[eventBufferSize++] = byte
            }
            TELNET_SE -> consumeAnd(State.IDLE) {
                // NOTE if size == 1 it's just SB with no content
                if (eventBufferSize > 1) {
                    onTelnetEvent(TelnetEvent(eventBuffer, 0, eventBufferSize))
                }
            }
            else -> consumeAnd(State.SB) // unexpected byte; just consume and ignore it
        }
    }

    private fun onTelnetEvent(command: Byte) {
        eventBuffer[0] = command
        onTelnetEvent(TelnetEvent(eventBuffer, 0, 1))
    }

    private fun onTelnetEvent(kind: Byte, data: Byte) {
        eventBuffer[0] = kind
        eventBuffer[1] = data
        onTelnetEvent(TelnetEvent(eventBuffer, 0, 2))
    }

    private inline fun consumeAnd(newState: State, block: () -> Unit = { /* nop */ }): Boolean {
        block()
        state = newState
        return true
    }

    private inline fun returnByteAnd(newState: State, block: () -> Unit = { /* nop */ }): Boolean {
        block()
        state = newState
        return false
    }
}