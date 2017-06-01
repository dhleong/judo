package net.dhleong.judo.net

import org.apache.commons.net.telnet.TelnetClient
import java.io.IOException
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
        try {
            client.disconnect()
            input.close()
            output.flush()
            output.close()
        } catch (e: IOException) {
            // ignore?
        }
    }

    override fun toString(): String {
        return "[${client.remoteAddress}:${client.remotePort}]"
    }
}