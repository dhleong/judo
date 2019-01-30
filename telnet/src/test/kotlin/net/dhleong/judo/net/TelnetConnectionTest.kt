package net.dhleong.judo.net

import com.nhaarman.mockito_kotlin.mock
import net.dhleong.judo.TestableJudoCore
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * @author dhleong
 */
class TelnetConnectionTest {
    @Test fun `Smoke test`() {
        TelnetConnection(
            judo = TestableJudoCore(),
            toString = "",
            socket = mock {  },
            input = "".byteInputStream(),
            output = ByteArrayOutputStream()
        )
    }
}