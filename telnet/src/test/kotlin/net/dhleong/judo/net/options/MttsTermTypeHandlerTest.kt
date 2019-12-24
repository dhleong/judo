package net.dhleong.judo.net.options

import assertk.assert
import assertk.assertThat
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

        assertThat(bytes).nextIsWill(TELNET_TELOPT_TERMINAL_TYPE)

        assertThat(bytes.get()).isEqualTo(TELNET_IAC)
        assertThat(bytes.get()).isEqualTo(TELNET_SB)
        assertThat(bytes.get()).isEqualTo(TELNET_TELOPT_TERMINAL_TYPE)
        assertThat(bytes.get()).isEqualTo(TELNET_IS)
        assertThat(bytes).nextIsString(JudoCore.CLIENT_NAME.toUpperCase())
        assertThat(bytes.get()).isEqualTo(TELNET_IAC)
        assertThat(bytes.get()).isEqualTo(TELNET_SE)

        // rotate on second request
        assertThat(bytes.get()).isEqualTo(TELNET_IAC)
        assertThat(bytes.get()).isEqualTo(TELNET_SB)
        assertThat(bytes.get()).isEqualTo(TELNET_TELOPT_TERMINAL_TYPE)
        assertThat(bytes.get()).isEqualTo(TELNET_IS)
        assertThat(bytes).nextIsString("Serenity")
        assertThat(bytes.get()).isEqualTo(TELNET_IAC)
        assertThat(bytes.get()).isEqualTo(TELNET_SE)

        assertThat(bytes).hasNoMore()
    }
}