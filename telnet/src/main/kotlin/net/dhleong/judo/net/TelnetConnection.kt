package net.dhleong.judo.net

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.net.options.GmcpHandler
import net.dhleong.judo.net.options.MccpInputStream
import net.dhleong.judo.net.options.MsdpHandler
import net.dhleong.judo.net.options.MttsTermTypeHandler
import net.dhleong.judo.net.options.WindowSizeHandler
import net.dhleong.judo.util.whenTrue
import java.net.Socket
import java.net.URI
import javax.net.ssl.SSLSocketFactory

/**
 * @author dhleong
 */
class TelnetConnection internal constructor(
    private val judo: IJudoCore,
    private val toString: String,
    private val socket: Closeable,
    input: InputStream,
    output: OutputStream,
    private val debug: Boolean = false,
    logRaw: Boolean = false
) : BaseConnection(logRaw) {

    class Factory(
        private val debug: Boolean = false,
        private val logRaw: Boolean = false
    ) : JudoConnection.Factory {
        override fun create(judo: IJudoCore, uri: URI): JudoConnection? =
            whenTrue(uri.scheme == "telnet") {
                TelnetConnection(
                    judo,
                    "[${uri.host}:${uri.port}]",
                    Socket(uri.host, uri.port),
                    debug = debug,
                    logRaw = logRaw
                )
            }
    }

    class SecureFactory(
        private val debug: Boolean = false,
        private val logRaw: Boolean = false
    ) : JudoConnection.Factory {
        override fun create(judo: IJudoCore, uri: URI): JudoConnection? =
            whenTrue(uri.scheme == "ssl") {
                TelnetConnection(
                    judo,
                    "[$uri]",
                    SSLSocketFactory.getDefault().createSocket(
                        uri.host, uri.port
                    ),
                    debug = debug,
                    logRaw = logRaw
                )
            }
    }

    private constructor(
        judo: IJudoCore, toString: String, socket: Socket,
        debug: Boolean, logRaw: Boolean
    ) : this(
        judo, toString, socket, socket.getInputStream(), socket.getOutputStream(),
        debug, logRaw
    )

    private val printDebug = { text: String ->
        if (debug) {
            judo.print(text)
        }
    }

    private val telnetClient = TelnetClient(
        input = MccpInputStream(input),
        output = output,
        printDebug = if (debug) printDebug else null
    )
    private val windowSizeHandler by lazy {
        WindowSizeHandler().also {
            telnetClient.register(it)
        }
    }

    private lateinit var gmcp: GmcpHandler
    private lateinit var msdp: MsdpHandler

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
        gmcp = GmcpHandler(judo, { debug }, printDebug)
        msdp = MsdpHandler(judo, { debug }, printDebug)

        register(object : TelnetOptionHandler(TELNET_TELOPT_ECHO, acceptRemoteDo = true) {
            override fun onRemoteDo(client: TelnetClient) {
                super.onRemoteDo(client)
                onEchoStateChanged?.invoke(true)
            }
            override fun onRemoteDont(client: TelnetClient) {
                super.onRemoteDont(client)
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