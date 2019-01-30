package net.dhleong.judo.net

import assertk.assert
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

        assert(bytes).nextIsWill(TELNET_TELOPT_NAWS)

        assert(bytes.get()).isEqualTo(TELNET_IAC)
        assert(bytes.get()).isEqualTo(TELNET_SB)
        assert(bytes.get()).isEqualTo(TELNET_TELOPT_NAWS)
        assert(bytes.short.toInt()).isEqualTo(22)
        assert(bytes.short.toInt()).isEqualTo(42)
    }

    @Test fun `Send large numbers`() {
        val bytes = WindowSizeHandler(422, 9001).sentBytesOn {
            onRemoteDo(it)
        }

        assert(bytes).nextIsWill(TELNET_TELOPT_NAWS)

        assert(bytes.get()).isEqualTo(TELNET_IAC)
        assert(bytes.get()).isEqualTo(TELNET_SB)
        assert(bytes.get()).isEqualTo(TELNET_TELOPT_NAWS)
        assert(bytes.short.toInt()).isEqualTo(422)
        assert(bytes.short.toInt()).isEqualTo(9001)
    }

    @Test fun `Duplicate IAC as necessary`() {
        val bytes = WindowSizeHandler(255, 0xffff).sentBytesOn {
            onRemoteDo(it)
        }

        assert(bytes).nextIsWill(TELNET_TELOPT_NAWS)

        assert(bytes.get()).isEqualTo(TELNET_IAC)
        assert(bytes.get()).isEqualTo(TELNET_SB)
        assert(bytes.get()).isEqualTo(TELNET_TELOPT_NAWS)

        // width:
        assert(bytes.get()).isEqualTo(0.toByte())

        assert(bytes.get()).isEqualTo(TELNET_IAC)
        assert(bytes.get()).isEqualTo(255.toByte())

        // height:
        assert(bytes.get()).isEqualTo(TELNET_IAC)
        assert(bytes.get()).isEqualTo(TELNET_IAC)

        assert(bytes.get()).isEqualTo(TELNET_IAC)
        assert(bytes.get()).isEqualTo(TELNET_IAC)
    }
}

