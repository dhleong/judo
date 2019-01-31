package net.dhleong.judo.net

class TelnetClient(
    input: InputStream,
    output: OutputStream,
    maxSubnegotiationSize: Int = 8192,
    private val printDebug: ((String) -> Unit)? = null
) {
    val inputStream: InputStream = TelnetInputStream(input, maxSubnegotiationSize, this::onTelnetEvent)
    val outputStream: OutputStream = SynchronizedOutputStream(output)

    private val handlers = arrayOfNulls<TelnetOptionHandler>(MAX_TELOPT_VALUE + 1)

    /**
     * NOTE: There can be only one handler of any given type
     */
    fun register(handler: TelnetOptionHandler): TelnetOptionHandler? = synchronized(handlers) {
        val index = handler.option.toIndex()
        val old = handlers[index]
        handlers[index] = handler

        handler.onAttach(this)

        return old
    }

    fun unregister(handler: TelnetOptionHandler): Boolean = synchronized(handlers) {
        val index = handler.option.toIndex()
        val old = handlers[index]
        if (old === handler) {
            handlers[index] = null
            true
        } else false
    }

    /**
     * Send a command that you build byte-by-byte
     */
    inline fun sendCommand(block: OutputStream.() -> Unit) {
        synchronized(outputStream) {
            outputStream.apply {
                write(TELNET_IAC)
                block()
                flush()
            }
        }
    }

    inline fun sendSubnegotiation(
        option: Byte,
        crossinline block: OutputStream.() -> Unit
    ) = sendCommand {
        write(TELNET_SB)
        write(option)
        block()
        write(TELNET_IAC)
        write(TELNET_SE)
    }

    internal fun sendOpt(cmd: Byte, option: Byte) = sendCommand {
        printDebug { "## TELNET > ${cmd.describe()} ${option.describe()}" }
        write(cmd)
        write(option)
    }

    private fun onTelnetEvent(event: TelnetEvent) {
        printDebug { "## TELNET < ${event.joinToString(" ") { it.describe() }}" }
        when (event[0]) {
            TELNET_DO -> handleCmdOpt(event, TELNET_WONT, TelnetOptionHandler::onRemoteDo)
            TELNET_DONT -> handleCmdOpt(event, onHandler = TelnetOptionHandler::onRemoteDont)
            TELNET_WILL -> handleCmdOpt(event, TELNET_DONT, TelnetOptionHandler::onRemoteWill)
            TELNET_WONT -> handleCmdOpt(event, onHandler = TelnetOptionHandler::onRemoteWont)

            TELNET_SB -> {
                if (event.length < 1) return // nop
                val opt = event[1]
                handlers[opt.toIndex()]?.onSubnegotiation(this, event)
            }
        }
    }

    private inline fun handleCmdOpt(
        event: TelnetEvent,
        refuseCode: Byte = 0.toByte(),
        onHandler: TelnetOptionHandler.(TelnetClient) -> Unit
    ) {
        if (event.length < 1) return // nop

        val opt = event[1]
        val handler = handlers[opt.toIndex()]
        if (handler != null) {
            handler.onHandler(this)
        } else if (refuseCode != 0.toByte()) {
            sendOpt(refuseCode, opt)
        }
    }

    fun close() {
        inputStream.close()
        outputStream.close()
    }

    private fun Byte.toIndex(): Int = toInt() and 0xff

    private inline fun printDebug(block: () -> String) {
        printDebug?.let {
            it(block())
        }
    }

    private fun Byte.describe() = when (this) {
        TELNET_IAC -> "IAC"
        TELNET_DONT -> "DONT"
        TELNET_DO -> "DO"
        TELNET_WONT -> "WONT"
        TELNET_WILL -> "WILL"
        TELNET_SB -> "SB"
        TELNET_SE -> "SE"

        TELNET_TELOPT_ECHO -> "ECHO"
        TELNET_TELOPT_TERMINAL_TYPE -> "TTYPE"
        TELNET_TELOPT_NAWS -> "NAWS"
        TELNET_TELOPT_MSDP -> "MSDP"
        TELNET_TELOPT_MCCP2 -> "MCCP"
        TELNET_TELOPT_GMCP -> "GMCP"

        70.toByte() -> "MSSP" // server status
        85.toByte() -> "MCCP1" // legacy; don't use

        90.toByte() -> "MSP" // mud sound
        91.toByte() -> "MXP" // mud extension
        93.toByte() -> "ZMP" // zenith mud

        239.toByte() -> "EOR" // used for prompt marking
        249.toByte() -> "GA" // used for prompt marking

        else -> (toInt() and 0xff).toString(16)
    }
}

private class SynchronizedOutputStream(
    private val delegate: OutputStream
) : OutputStream() {

    override fun write(b: Int) = synchronized(this) {
        delegate.write(b)
    }

    override fun write(b: ByteArray?) = synchronized(this) {
        super.write(b)
    }

    override fun write(b: ByteArray?, off: Int, len: Int) = synchronized(this) {
        super.write(b, off, len)
    }

}

