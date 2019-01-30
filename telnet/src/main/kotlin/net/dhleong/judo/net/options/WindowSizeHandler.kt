package net.dhleong.judo.net.options

import net.dhleong.judo.net.TELNET_TELOPT_NAWS
import net.dhleong.judo.net.TelnetClient
import net.dhleong.judo.net.TelnetOptionHandler
import net.dhleong.judo.net.writeShort
import java.util.concurrent.atomic.AtomicBoolean

class WindowSizeHandler(
    private var width: Int = 0,
    private var height: Int = 0
) : TelnetOptionHandler(
    TELNET_TELOPT_NAWS,
    sendWill = true,
    acceptRemoteDo = true
) {

    private lateinit var client: TelnetClient
    private val enabled = AtomicBoolean(false)

    override fun onAttach(client: TelnetClient) {
        super.onAttach(client)
        this.client = client
        client.trySendSize()
    }

    override fun onRemoteDo(client: TelnetClient) {
        super.onRemoteDo(client)
        enabled.set(true)
        client.trySendSize()
    }

    override fun onRemoteDont(client: TelnetClient) {
        super.onRemoteDont(client)
        enabled.set(false)
    }

    fun setSize(width: Int, height: Int) {
        if (this.width == width && this.height == height) {
            // no change
            return
        }

        this.width = width
        this.height = height
        client.trySendSize()
    }

    private fun TelnetClient.trySendSize() {
        if (!enabled.get()) return
        sendSubnegotiation {
            writeShort(width)
            writeShort(height)
        }
    }

}

