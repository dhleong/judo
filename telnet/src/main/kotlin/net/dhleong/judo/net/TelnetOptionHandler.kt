package net.dhleong.judo.net

/**
 * @author dhleong
 */
open class TelnetOptionHandler(
    val option: Byte,
    internal val sendWill: Boolean = false,
    internal val sendDo: Boolean = false,
    internal val acceptRemoteWill: Boolean = false,
    internal val acceptRemoteDo: Boolean = false
) {

    open fun onAttach(client: TelnetClient) {
        if (sendWill) client.sendWill()
        if (sendDo) client.sendDo()
    }

    open fun onRemoteDo(client: TelnetClient, event: TelnetEvent) {
        if (acceptRemoteDo) client.sendWill()
        else client.sendWont()
    }

    open fun onRemoteDont(client: TelnetClient, event: TelnetEvent) {
        // nop, otherwise?
        client.sendWont()
    }

    open fun onRemoteWill(client: TelnetClient, event: TelnetEvent) {
        if (acceptRemoteWill) client.sendDo()
        else client.sendDont()
    }

    open fun onRemoteWont(client: TelnetClient, event: TelnetEvent) {
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

    private fun TelnetClient.sendDo() = sendOpt(TELNET_DO, option)
    private fun TelnetClient.sendDont() = sendOpt(TELNET_DONT, option)
    private fun TelnetClient.sendWill() = sendOpt(TELNET_WILL, option)
    private fun TelnetClient.sendWont() = sendOpt(TELNET_WONT, option)

}