package net.dhleong.judo.net

import org.apache.commons.net.telnet.TelnetClient
import java.io.InputStream
import java.io.OutputStream

class CommonsNetConnection(address: String, port: Int) : Connection() {
    override val input: InputStream
    override val output: OutputStream

    private val client = TelnetClient()

    init {
        client.connect(address, port)
        input = client.inputStream
        output = client.outputStream
    }

    override fun close() {
        client.disconnect()
        input.close()
        output.close()
    }

    override fun toString(): String {
        return "[${client.remoteAddress}:${client.remotePort}"
    }
}