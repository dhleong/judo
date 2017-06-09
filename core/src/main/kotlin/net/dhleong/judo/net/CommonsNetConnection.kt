package net.dhleong.judo.net

import org.apache.commons.net.telnet.EchoOptionHandler
import org.apache.commons.net.telnet.SimpleOptionHandler
import org.apache.commons.net.telnet.TelnetClient
import org.apache.commons.net.telnet.TelnetNotificationHandler
import org.apache.commons.net.telnet.TelnetOption
import org.apache.commons.net.telnet.WindowSizeOptionHandler
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class CommonsNetConnection(
    private val address: String, private val port: Int,
    terminalType: String = "xterm-256color",
    private val echo: (String) -> Unit
) : Connection() {

    override val input: InputStream
    override val output: OutputStream

    // NOTE: it's a bit weird storing this value in the socketFactory,
    // but we need to update it somehow....
    var debug: Boolean
        get() = socketFactory.debug
        set(value) {
            socketFactory.debug = value
        }

    private val client = TelnetClient(terminalType)
    private val socketFactory = MccpHandlingSocketFactory(echo)

    private var echoStateChanges = 0

    private var lastWidth = -1
    private var lastHeight = -1

    init {
        client.setSocketFactory(socketFactory)
        client.connect(address, port)

        input = client.inputStream
        output = client.outputStream

        client.addOptionHandler(EchoOptionHandler(false, false, false, true))
        client.addOptionHandler(SimpleOptionHandler(TELNET_TELOPT_MCCP2.toInt(), false, false, true, true))
        client.registerNotifHandler { negotiation_code, option_code ->
            if (debug) {
                echo("## TELNET ${stringify(negotiation_code)} ${stringifyOption(option_code)}")
            }

            if (option_code == TelnetOption.ECHO) {
                if (echoStateChanges > 5) {
                    // this is probably enough; just ignore it
                    // (nop)
                } else {
                    ++echoStateChanges
                    val doEcho = echoStateChanges >= 5 ||
                        negotiation_code != TelnetNotificationHandler.RECEIVED_WILL
                    onEchoStateChanged?.invoke(doEcho)
                }
            }
        }
    }

    override fun close() {
        try {
            client.disconnect()
        } catch (e: IOException) {
            // ignore?
        }
    }

    override fun setWindowSize(width: Int, height: Int) {
        if (width == lastWidth && height == lastHeight) return

        val windowSizeHandler = WindowSizeOptionHandler(width, height, false, false, true, false)
        if (lastWidth != -1) {
            // delete the old handler; if lastWidth (or lastHeight) == -1 there
            // is none, and deleting a non-existing handler is apparently an error
            client.deleteOptionHandler(windowSizeHandler.optionCode)
        }

        lastWidth = width
        lastHeight = height
        client.addOptionHandler(windowSizeHandler)
        client.sendSubnegotiation(windowSizeHandler.startSubnegotiationLocal())
    }

    override fun toString(): String {
        return "[$address:$port]"
    }

    private fun stringify(negotiationCode: Int): String =
        when (negotiationCode) {
            TelnetNotificationHandler.RECEIVED_WILL -> "WILL"
            TelnetNotificationHandler.RECEIVED_WONT -> "WONT"
            TelnetNotificationHandler.RECEIVED_DO -> "DO"
            TelnetNotificationHandler.RECEIVED_DONT -> "DONT"
            TelnetNotificationHandler.RECEIVED_COMMAND -> "COMMAND"
            else -> "[$negotiationCode]"
        }

    private fun stringifyOption(optionCode: Int): String {
        val known = TelnetOption.getOption(optionCode)
        if (known == "UNASSIGNED") {
            return "[$optionCode]"
        }
        return known
    }

}

