package net.dhleong.judo.net

import net.dhleong.judo.JudoCore
import net.dhleong.judo.JudoRendererInfo
import org.apache.commons.net.telnet.TelnetOption
import org.apache.commons.net.telnet.TelnetOptionHandler

class MttsTermTypeHandler(
    private val info: JudoRendererInfo,
    private val echoDebug: (String) -> Unit
) : TelnetOptionHandler(
    TelnetOption.TERMINAL_TYPE,
    false,
    false,
    true,
    false
) {

    private enum class State {
        CLIENT_NAME,
        TERM_TYPE,
        MTTS_BITVECTOR
    }

    private var state = State.CLIENT_NAME

    override fun answerSubnegotiation(suboptionData: IntArray?, suboptionLength: Int): IntArray {
        val name = getNameForCurrentState()
        advanceState()

        echoDebug("## TELNET > IAC SB TTYPE IS $name")
        return with(name.map { it.toInt() }.toMutableList()) {
            TELNET_IAC
            add(0, TelnetOption.TERMINAL_TYPE)
            add(1, TELNET_IS.toInt())
            toIntArray()
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