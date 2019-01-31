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

    @Test fun `Respond to DO if we didn't send initial WILL`() {
        val handler = TelnetOptionHandler(
            42,
            acceptRemoteDo = true
        )

        // verify we don't send anything on attach
        val attach = handler.sentBytesOn {
            onAttach(it)
        }
        assert(attach).all {
            hasNoMore()
        }

        // only on do
        val bytes = handler.sentBytesOn {
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