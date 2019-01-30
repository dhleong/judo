package net.dhleong.judo.net

import net.dhleong.judo.JudoCore
import net.dhleong.judo.JudoRendererInfo

class MttsTermTypeHandler(
    private val info: JudoRendererInfo,
    private val printDebug: (String) -> Unit
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

    override fun onRemoteDont(client: TelnetClient, event: TelnetEvent) {
        super.onRemoteDont(client, event)
        reset()
    }

    override fun onSubnegotiation(client: TelnetClient, event: TelnetEvent) {
        val name = getNameForCurrentState()
        advanceState()

        printDebug("## TELNET > IAC SB TTYPE IS $name")
        client.sendSubnegotiation {
            write(TELNET_IS)
            write(name)
        }
    }

    fun reset() {
        state = State.CLIENT_NAME
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
