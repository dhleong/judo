package net.dhleong.judo.net

import java.io.BufferedInputStream

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

    override fun read(): Int {
        var read: Int
        do {
            read = delegate.read()
        } while (read != -1 && handleTelnetCommand((read and 0xff).toByte()))
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
                eventBuffer[++eventBufferSize] = byte
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