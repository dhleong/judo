package net.dhleong.judo.net

import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream
import java.net.InetAddress
import java.net.Socket
import java.util.zip.Inflater
import javax.net.SocketFactory

const val TELNET_TELOPT_MCCP2 = 86.toByte()

class MccpInputStream(val socket: MccpHandlingSocket, delegate: InputStream): InputStream() {

    private val inputStream = PushbackInputStream(delegate, 8192)
    private val inflater = Inflater()
    private val buffer = ByteArray(1024)

    override fun read(): Int = throw UnsupportedOperationException("Use read(byte[])")

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (socket.compressEnabled) {
            if (!inflater.finished()) {
                return inflateInto(b, off, len)
            }

            // server terminated compression
            socket.compressEnabled = false
            debug("## MCCP Disabled")
        }

        // enable mccp?
        var read = inputStream.read(b, off, len)

        for (i in 0..(read - 4)) {
            if (b[i] != TELNET_IAC) continue

            // mccp?
            if (b[i + 1].toInt() == TELNET_SB.toInt() && b[i + 2] == TELNET_TELOPT_MCCP2) {
                socket.compressEnabled = true
                debug("## MCCP Enabled")

                val afterSubEnd = i + 5 // 3 = IAC; 4 = SE
                val left = read - afterSubEnd
                if (left > 0) {
                    read = afterSubEnd
                    inputStream.unread(b, afterSubEnd, left)
                }
                break
            }
        }
        return read

    }

    private fun inflateInto(b: ByteArray, off: Int, len: Int): Int {
        val read = inputStream.read(buffer)
        if (read == -1) return -1

        inflater.setInput(buffer, 0, read)

        var inflated = 0
        var offset = off
        var available = len
        while (inflater.remaining > 0 && available > 0) {
            val readBytes = inflater.inflate(b, offset, available)
            inflated += readBytes
            available -= readBytes
            offset += readBytes

            if (readBytes == 0) {
                if (inflater.finished()) break
                if (inflater.needsDictionary()) {
                    return -1
                }
            }
        }

        if (inflater.remaining > 0) {
            val start = read - inflater.remaining
            try {
                inputStream.unread(buffer, start, inflater.remaining)
            } catch (e: IOException) {
                throw IOException("Unable to unread ${inflater.remaining} bytes", e)
            }
        }

        return inflated
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
