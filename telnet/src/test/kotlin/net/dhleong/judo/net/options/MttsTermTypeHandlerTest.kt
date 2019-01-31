package net.dhleong.judo.net.options

import assertk.assert
import assertk.assertions.isEqualTo
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import net.dhleong.judo.JudoCore
import net.dhleong.judo.JudoRendererInfo
import net.dhleong.judo.net.TELNET_IAC
import net.dhleong.judo.net.TELNET_IS
import net.dhleong.judo.net.TELNET_SB
import net.dhleong.judo.net.TELNET_SE
import net.dhleong.judo.net.TELNET_SEND
import net.dhleong.judo.net.TELNET_TELOPT_TERMINAL_TYPE
import net.dhleong.judo.net.TelnetEvent
import net.dhleong.judo.net.hasNoMore
import net.dhleong.judo.net.nextIsString
import net.dhleong.judo.net.nextIsWill
import net.dhleong.judo.net.sentBytesOn
import org.junit.Test
import java.util.EnumSet

/**
 * @author dhleong
 */
class MttsTermTypeHandlerTest {
    @Test fun `Respond to DO`() {
        val bytes = MttsTermTypeHandler(mock {
            on { terminalType } doReturn "Serenity"
            on { capabilities } doReturn EnumSet.allOf(JudoRendererInfo.Capabilities::class.java)
        }).sentBytesOn {
            onRemoteDo(it)
            onSubnegotiation(it, TelnetEvent(byteArrayOf(
                TELNET_SB,
                TELNET_TELOPT_TERMINAL_TYPE,
                TELNET_SEND
            )))
        }

        assert(bytes).nextIsWill(TELNET_TELOPT_TERMINAL_TYPE)

        assert(bytes.get()).isEqualTo(TELNET_IAC)
        assert(bytes.get()).isEqualTo(TELNET_SB)
        assert(bytes.get()).isEqualTo(TELNET_TELOPT_TERMINAL_TYPE)
        assert(bytes.get()).isEqualTo(TELNET_IS)
        assert(bytes).nextIsString(JudoCore.CLIENT_NAME.toUpperCase())
        assert(bytes.get()).isEqualTo(TELNET_IAC)
        assert(bytes.get()).isEqualTo(TELNET_SE)

        // rotate on second request
        assert(bytes.get()).isEqualTo(TELNET_IAC)
        assert(bytes.get()).isEqualTo(TELNET_SB)
        assert(bytes.get()).isEqualTo(TELNET_TELOPT_TERMINAL_TYPE)
        assert(bytes.get()).isEqualTo(TELNET_IS)
        assert(bytes).nextIsString("Serenity")
        assert(bytes.get()).isEqualTo(TELNET_IAC)
        assert(bytes.get()).isEqualTo(TELNET_SE)

        assert(bytes).hasNoMore()
    }
}