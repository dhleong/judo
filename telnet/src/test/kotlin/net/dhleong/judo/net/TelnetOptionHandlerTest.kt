package net.dhleong.judo.net

import assertk.all
import assertk.assert
import org.junit.Test

/**
 * @author dhleong
 */
class TelnetOptionHandlerTest {
    @Test fun `Don't cause infinite will-do loops`() {
        val handler = TelnetOptionHandler(
           42,
            sendWill = true,
            acceptRemoteDo = true
        )

        val bytes = handler.sentBytesOn {
            onAttach(it)
            onRemoteDo(it)
        }

        assert(bytes).all {
            nextIsWill(42)
            hasNoMore()
        }
    }

    @Test fun `Don't cause infinite do-will loops`() {
        val handler = TelnetOptionHandler(
            42,
            sendDo = true,
            acceptRemoteWill = true
        )

        val bytes = handler.sentBytesOn {
            onAttach(it)
            onRemoteWill(it)
        }

        assert(bytes).all {
            nextIsDo(42)
            hasNoMore()
        }
    }
}