package net.dhleong.judo.net

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.net.options.GmcpHandler
import net.dhleong.judo.net.options.MccpInputStream
import net.dhleong.judo.net.options.MsdpHandler
import net.dhleong.judo.net.options.MttsTermTypeHandler
import net.dhleong.judo.net.options.WindowSizeHandler
import java.net.Socket

/**
 * @author dhleong
 */
class TelnetConnection private constructor(
    private val judo: IJudoCore,
    private val toString: String,
    private val socket: Closeable,
    rawInput: InputStream,
    output: OutputStream
) : BaseConnection(false) { // TODO debug mode (?)

    class Factory : JudoConnection.Factory {
        override fun create(judo: IJudoCore, address: String, port: Int): JudoConnection {
            return TelnetConnection(judo, "[$address:$port]", Socket(address, port))
        }
    }

    private constructor(judo: IJudoCore, toString: String, socket: Socket) : this(
        judo, toString, socket, socket.getInputStream(), socket.getOutputStream()
    )

    private val telnetClient = TelnetClient(MccpInputStream(rawInput), output)
    private val windowSizeHandler by lazy {
        WindowSizeHandler().also {
            telnetClient.register(it)
        }
    }

    private lateinit var gmcp: GmcpHandler
    private lateinit var msdp: MsdpHandler

    // TODO debug?
    var debug = false // FIXME
    override val input = telnetClient.inputStream
    override val output = telnetClient.outputStream

    override val isMsdpEnabled: Boolean
        get() = msdp.isMsdpEnabled
    override val isGmcpEnabled: Boolean
        get() = gmcp.isGmcpEnabled

    init {
        telnetClient.registerHandlers()
    }

    override fun close() {
        super.close()
        telnetClient.close()
        socket.close()
    }

    override fun setWindowSize(width: Int, height: Int) {
        windowSizeHandler.setSize(width, height)
    }

    private fun TelnetClient.registerHandlers() {
        val printDebug = { text: String ->
            if (debug) {
                print(text)
            }
        }
        gmcp = GmcpHandler(judo, { debug }, printDebug)
        msdp = MsdpHandler(judo, { debug }, printDebug)

        register(object : TelnetOptionHandler(TELNET_TELOPT_ECHO, acceptRemoteDo = true) {
            override fun onRemoteDo(client: TelnetClient, event: TelnetEvent) {
                super.onRemoteDo(client, event)
                onEchoStateChanged?.invoke(true)
            }
            override fun onRemoteDont(client: TelnetClient, event: TelnetEvent) {
                super.onRemoteDont(client, event)
                onEchoStateChanged?.invoke(false)
            }
        })

        register(MttsTermTypeHandler(judo.renderer, printDebug))
        register(TelnetOptionHandler(TELNET_TELOPT_MCCP2,
            acceptRemoteDo = true,
            acceptRemoteWill = true
        ))

        register(gmcp)
        register(msdp)
    }

    override fun toString(): String = toString

}