package net.dhleong.judo.net

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.net.JudoConnection.Companion.DEFAULT_CONNECT_TIMEOUT
import org.apache.commons.net.telnet.EchoOptionHandler
import org.apache.commons.net.telnet.SimpleOptionHandler
import org.apache.commons.net.telnet.TelnetClient
import org.apache.commons.net.telnet.TelnetNotificationHandler
import org.apache.commons.net.telnet.TelnetOption
import org.apache.commons.net.telnet.WindowSizeOptionHandler
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class CommonsNetConnection private constructor(
    judo: IJudoCore,
    private val address: String,
    private val port: Int,
    private val echo: (String) -> Unit
) : BaseConnection() {
    class Factory(val debug: Boolean = false) : JudoConnection.Factory {
        override fun create(judo: IJudoCore, address: String, port: Int): JudoConnection {
            return CommonsNetConnection(judo, address, port) { judo.echo(it) }.also {
                it.debug = debug
            }
        }
    }

    override val input: InputStream
    override val output: OutputStream

    override val isMsdpEnabled: Boolean
        get() = msdp.isMsdpEnabled
    override val isGmcpEnabled: Boolean
        get() = gmcp.isGmcpEnabled

    // NOTE: it's a bit weird storing this value in the socketFactory,
    // but we need to update it somehow....
    var debug: Boolean
        get() = socketFactory.debug
        set(value) {
            socketFactory.debug = value
        }

    private val client = TelnetClient()
    private val socketFactory = MccpHandlingSocketFactory(echo)

    private val gmcp: GmcpHandler
    private val msdp: MsdpHandler

    private var echoStateChanges = 0

    private var lastWidth = -1
    private var lastHeight = -1

    init {
        client.connectTimeout = DEFAULT_CONNECT_TIMEOUT
        client.setSocketFactory(socketFactory)
        client.connect(address, port)

        input = MsdpHandler.wrap(client.inputStream)
        output = client.outputStream

        val echoDebug = { text: String ->
            if (debug) {
                echo(text)
            }
        }

        val mtts = MttsTermTypeHandler(judo.renderer, echoDebug)
        msdp = MsdpHandler(judo, { debug }, echoDebug)
        gmcp = GmcpHandler(judo, { debug }, echoDebug)
        client.addOptionHandler(mtts)
        client.addOptionHandler(EchoOptionHandler(false, false, false, true))
        client.addOptionHandler(gmcp)
        client.addOptionHandler(msdp)
        client.addOptionHandler(SimpleOptionHandler(TELNET_TELOPT_MCCP2.toInt(), false, false, true, true))
        client.registerNotifHandler { negotiation_code, option_code ->
            echoDebug("## TELNET < ${stringify(negotiation_code)} ${stringifyOption(option_code)}")

            when (option_code) {
                TelnetOption.ECHO -> {
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

                TelnetOption.TERMINAL_TYPE -> {
                    if (negotiation_code == TelnetNotificationHandler.RECEIVED_DONT) {
                        mtts.reset()
                    }
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

    override suspend fun send(line: String) {
        if (isTelnetSubsequence(line)) {
            // hold commons.net's hand so it doesn't stomp on the
            // user's IAC codes
            val intArrayLength = line.length - 4 // IAC SE / IAC SB
            val asIntArray = IntArray(intArrayLength)

            for (i in 2 until line.length - 2) {
                asIntArray[i - 2] = line[i].toInt()
            }

            client.sendSubnegotiation(asIntArray)
        } else {
            // regular line; send it regularly
            super.send(line)
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

    private fun stringifyOption(optionCode: Int): String =
        when (optionCode) {
            TELNET_TELOPT_MSDP.toInt() -> "MSDP"
            70 -> "MSSP" // server status
            85 -> "MCCP1" // legacy; don't use
            TELNET_TELOPT_MCCP2.toInt() -> "MCCP"
            90 -> "MSP" // mud sound
            91 -> "MXP" // mud extension
            93 -> "ZMP" // zenith mud
            TELNET_TELOPT_GMCP -> "GMCP"
            239 -> "EOR" // used for prompt marking
            249 -> "GA" // used for prompt marking
            else -> {
                val known = TelnetOption.getOption(optionCode)
                if (known == "UNASSIGNED") "[$optionCode]"
                else known
            }
        }

}


