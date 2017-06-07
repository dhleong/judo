package net.dhleong.judo.net

import org.apache.commons.net.telnet.EchoOptionHandler
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

    var debug = false

    private val client = TelnetClient(terminalType)

    private var echoStateChanges = 0

    init {
        client.connect(address, port)

        input = client.inputStream
        output = client.outputStream
    }

    override fun close() {
        try {
            client.disconnect()
        } catch (e: IOException) {
            // ignore?
        }
    }

    override fun setWindowSize(width: Int, height: Int) {
        client.addOptionHandler(WindowSizeOptionHandler(width, height))
        client.addOptionHandler(EchoOptionHandler(false, false, false, true))
        client.registerNotifHandler { negotiation_code, option_code ->
            if (debug) {
                echo("## TELNET ${stringify(negotiation_code)} ${TelnetOption.getOption(option_code)}")
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

    override fun toString(): String {
        return "[$address:$port]"
    }

    private fun stringify(option_code: Int): String =
        when (option_code) {
            TelnetNotificationHandler.RECEIVED_WILL -> "WILL"
            TelnetNotificationHandler.RECEIVED_WONT -> "WONT"
            TelnetNotificationHandler.RECEIVED_DO -> "DO"
            TelnetNotificationHandler.RECEIVED_DONT -> "DONT"
            TelnetNotificationHandler.RECEIVED_COMMAND -> "COMMAND"
            else -> "[$option_code]"
        }

}