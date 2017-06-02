package net.dhleong.judo.net

import org.apache.commons.net.telnet.TelnetClient
import org.apache.commons.net.telnet.WindowSizeOptionHandler
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class CommonsNetConnection(
    private val address: String, private val port: Int,
    terminalType: String = "xterm-256color"
) : Connection() {
    override val input: InputStream
    override val output: OutputStream

    private val client = TelnetClient(terminalType)

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
    }

    override fun toString(): String {
        return "[$address:$port]"
    }
}