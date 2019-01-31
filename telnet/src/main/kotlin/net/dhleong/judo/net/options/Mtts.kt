package net.dhleong.judo.net.options

import net.dhleong.judo.JudoCore
import net.dhleong.judo.JudoRendererInfo
import net.dhleong.judo.net.MTTS_256COLOR
import net.dhleong.judo.net.MTTS_ANSI
import net.dhleong.judo.net.MTTS_UTF8
import net.dhleong.judo.net.MTTS_VT100
import net.dhleong.judo.net.TELNET_IS
import net.dhleong.judo.net.TELNET_TELOPT_TERMINAL_TYPE
import net.dhleong.judo.net.TelnetClient
import net.dhleong.judo.net.TelnetEvent
import net.dhleong.judo.net.TelnetOptionHandler
import net.dhleong.judo.net.write

class MttsTermTypeHandler(
    private val info: JudoRendererInfo,
    private val printDebug: (String) -> Unit = { /* nop */ }
) : TelnetOptionHandler(
    TELNET_TELOPT_TERMINAL_TYPE,
    acceptRemoteDo = true
) {

    private enum class State {
        CLIENT_NAME,
        TERM_TYPE,
        MTTS_BITVECTOR
    }

    private var state = State.CLIENT_NAME

    override fun onRemoteDo(client: TelnetClient) {
        super.onRemoteDo(client)
        sendType(client)
    }

    override fun onRemoteDont(client: TelnetClient) {
        super.onRemoteDont(client)
        reset()
    }

    override fun onSubnegotiation(client: TelnetClient, event: TelnetEvent) {
        sendType(client)
    }

    fun reset() {
        state = State.CLIENT_NAME
    }

    private fun sendType(client: TelnetClient) {
        val name = getNameForCurrentState()
        advanceState()

        printDebug("## TELNET > IAC SB TTYPE IS $name")
        client.sendSubnegotiation {
            write(TELNET_IS)
            write(name)
        }
    }

    private fun advanceState() {
        state = when (state) {
            State.CLIENT_NAME -> State.TERM_TYPE
            else -> State.MTTS_BITVECTOR
        }
    }

    private fun getNameForCurrentState(): String =
        when (state) {
            State.CLIENT_NAME -> JudoCore.CLIENT_NAME.toUpperCase()
            State.TERM_TYPE -> info.terminalType
            State.MTTS_BITVECTOR -> "MTTS ${buildBitVector()}"
        }

    private fun buildBitVector(): Int {
        var vector = MTTS_ANSI // we always support ansi

        if (info.capabilities.contains(JudoRendererInfo.Capabilities.UTF8)) {
            vector += MTTS_UTF8
        }

        if (info.capabilities.contains(JudoRendererInfo.Capabilities.VT100)) {
            vector += MTTS_VT100
        }

        if (info.capabilities.contains(JudoRendererInfo.Capabilities.COLOR_256)) {
            vector += MTTS_256COLOR
        }

        return vector
    }

}
