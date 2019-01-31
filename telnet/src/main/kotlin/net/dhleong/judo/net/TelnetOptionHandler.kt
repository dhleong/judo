package net.dhleong.judo.net

import java.util.concurrent.atomic.AtomicInteger

/**
 * @author dhleong
 */
open class TelnetOptionHandler(
    val option: Byte,
    private val sendWill: Boolean = false,
    private val sendDo: Boolean = false,
    internal val acceptRemoteWill: Boolean = false,
    internal val acceptRemoteDo: Boolean = false
) {

    private val localWill = AtomicInteger(1)
    private val localDo = AtomicInteger(1)

    open fun onAttach(client: TelnetClient) {
        if (sendWill) client.sendWillIfNeeded()
        if (sendDo) client.sendDoIfNeeded()
    }

    open fun onRemoteDo(client: TelnetClient) {
        if (acceptRemoteDo) client.sendWillIfNeeded()
        else client.sendWont()
    }

    open fun onRemoteDont(client: TelnetClient) {
        // nop, otherwise?
        client.sendWont()
    }

    open fun onRemoteWill(client: TelnetClient) {
        if (acceptRemoteWill) client.sendDoIfNeeded()
        else client.sendDont()
    }

    open fun onRemoteWont(client: TelnetClient) {
        // nop ?
    }

    /**
     * `event[0]` will always be `SB`, and `event[1]` will always be
     * valid (IE: will not result in an IndexOutOfBoundsException)
     */
    open fun onSubnegotiation(client: TelnetClient, event: TelnetEvent) {
        // nop
    }

    protected inline fun TelnetClient.sendSubnegotiation(crossinline block: OutputStream.() -> Unit) =
        sendSubnegotiation(option, block)

    private fun TelnetClient.sendDont() = sendOpt(TELNET_DONT, option)
    private fun TelnetClient.sendWont() = sendOpt(TELNET_WONT, option)
    private fun TelnetClient.sendWillIfNeeded() = sendIfNeeded(localWill, TELNET_WILL)
    private fun TelnetClient.sendDoIfNeeded() = sendIfNeeded(localDo, TELNET_DO)

    private fun TelnetClient.sendIfNeeded(queue: AtomicInteger, opt: Byte) {
        val queueSize = queue.getAndDecrement()
        if (queueSize > 0) {
            sendOpt(opt, option)
        }
    }

}