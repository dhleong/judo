package net.dhleong.judo.net

import assertk.assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.dhleong.judo.net.options.WindowSizeHandler
import org.junit.Test

/**
 * @author dhleong
 */
class WindowSizeHandlerTest {
    @Test fun `Send small numbers`() {
        val bytes = WindowSizeHandler(22, 42).sentBytesOn {
            onRemoteDo(it)
        }

        assertThat(bytes).nextIsWill(TELNET_TELOPT_NAWS)

        assertThat(bytes.get()).isEqualTo(TELNET_IAC)
        assertThat(bytes.get()).isEqualTo(TELNET_SB)
        assertThat(bytes.get()).isEqualTo(TELNET_TELOPT_NAWS)
        assertThat(bytes.short.toInt()).isEqualTo(22)
        assertThat(bytes.short.toInt()).isEqualTo(42)
    }

    @Test fun `Send large numbers`() {
        val bytes = WindowSizeHandler(422, 9001).sentBytesOn {
            onRemoteDo(it)
        }

        assertThat(bytes).nextIsWill(TELNET_TELOPT_NAWS)

        assertThat(bytes.get()).isEqualTo(TELNET_IAC)
        assertThat(bytes.get()).isEqualTo(TELNET_SB)
        assertThat(bytes.get()).isEqualTo(TELNET_TELOPT_NAWS)
        assertThat(bytes.short.toInt()).isEqualTo(422)
        assertThat(bytes.short.toInt()).isEqualTo(9001)
    }

    @Test fun `Duplicate IAC as necessary`() {
        val bytes = WindowSizeHandler(255, 0xffff).sentBytesOn {
            onRemoteDo(it)
        }

        assertThat(bytes).nextIsWill(TELNET_TELOPT_NAWS)

        assertThat(bytes.get()).isEqualTo(TELNET_IAC)
        assertThat(bytes.get()).isEqualTo(TELNET_SB)
        assertThat(bytes.get()).isEqualTo(TELNET_TELOPT_NAWS)

        // width:
        assertThat(bytes.get()).isEqualTo(0.toByte())

        assertThat(bytes.get()).isEqualTo(TELNET_IAC)
        assertThat(bytes.get()).isEqualTo(255.toByte())

        // height:
        assertThat(bytes.get()).isEqualTo(TELNET_IAC)
        assertThat(bytes.get()).isEqualTo(TELNET_IAC)

        assertThat(bytes.get()).isEqualTo(TELNET_IAC)
        assertThat(bytes.get()).isEqualTo(TELNET_IAC)
    }
}

