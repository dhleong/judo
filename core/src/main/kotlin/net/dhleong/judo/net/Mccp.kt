package net.dhleong.judo.net

import java.io.InputStream
import java.net.InetAddress
import java.net.Socket
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import javax.net.SocketFactory

val TELNET_IAC = 255.toByte()
val TELNET_SB = 250.toByte()

val TELNET_TELOPT_MCCP2 = 86.toByte()

class MccpInputStream(val socket: MccpHandlingSocket, val delegate: InputStream): InputStream() {

    private val inflater = Inflater()
    private val inflaterStream: InflaterInputStream by lazy { InflaterInputStream(delegate, inflater) }

    override fun read(): Int = throw UnsupportedOperationException("Use read(byte[])")

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (socket.compressEnabled) {
            if (!inflater.finished()) {
                return inflaterStream.read(b, off, len)
            }

            // server terminated compression
            socket.compressEnabled = false
            debug("## MCCP Disabled")
        }

        // enable mccp?
        val read = delegate.read(b, off, len)
        val iac = b.indexOf(TELNET_IAC)
        if (iac in 0..(read - 4)) {
            // mccp?
            if (b[iac+1] == TELNET_SB && b[iac+2] == TELNET_TELOPT_MCCP2) {
                socket.compressEnabled = true
                debug("## MCCP Enabled")
            }
        }
        return read

    }

    private fun debug(text: String) {
        if (socket.mccp.debug) {
            socket.mccp.echo(text)
        }
    }
}

class MccpHandlingSocket : Socket {

    internal var compressEnabled = false
    internal val mccpInputStream: MccpInputStream by lazy { MccpInputStream(this, super.getInputStream()) }

    internal val mccp: MccpHandlingSocketFactory

    constructor(mccp: MccpHandlingSocketFactory) : super() { this.mccp = mccp }
    constructor(mccp: MccpHandlingSocketFactory, host: String?, port: Int) : super(host, port) { this.mccp = mccp }
    constructor(mccp: MccpHandlingSocketFactory, host: InetAddress?, port: Int)
        : super(host, port) { this.mccp = mccp }
    constructor(mccp: MccpHandlingSocketFactory, host: String?, port: Int, localAddress: InetAddress?, localPort: Int)
        : super(host, port, localAddress, localPort) { this.mccp = mccp }
    constructor(mccp: MccpHandlingSocketFactory, host: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int)
        : super(host, port, localAddress, localPort) { this.mccp = mccp }

    override fun getInputStream(): InputStream = mccpInputStream
}

class MccpHandlingSocketFactory(internal val echo: (String) -> Unit) : SocketFactory() {

    var debug = false

    override fun createSocket(): Socket =
        MccpHandlingSocket(this)

    override fun createSocket(host: String?, port: Int): Socket =
        MccpHandlingSocket(this, host, port)

    override fun createSocket(host: String?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
        MccpHandlingSocket(this, host, port, localAddress, localPort)

    override fun createSocket(host: InetAddress?, port: Int): Socket =
        MccpHandlingSocket(this, host, port)

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
        MccpHandlingSocket(this, address, port, localAddress, localPort)

}