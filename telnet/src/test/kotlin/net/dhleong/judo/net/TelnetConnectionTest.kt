package net.dhleong.judo.net

import assertk.assert
import assertk.assertions.isTrue
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import net.dhleong.judo.TestableJudoCore
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

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

    @Test(timeout = 2000) fun `Stop reading on close`() {
        val socket = mock<Closeable> {  }
        val toRead = PipedOutputStream()
        val conn = TelnetConnection(
            judo = TestableJudoCore(),
            toString = "",
            socket = socket,
            input = PipedInputStream(toRead),
            output = ByteArrayOutputStream()
        )

        val calledOnDisconnect = AtomicBoolean(false)
        conn.onDisconnect = { calledOnDisconnect.set(true) }

        val latch = CountDownLatch(1)
        thread {
            conn.forEachLine {
                // ignore the line
            }

            // finished reading
            latch.countDown()
        }

        // wait until the thread starts reading
        Thread.sleep(50)

        conn.close()
        verify(socket, times(1)).close()

        latch.await()
        assert(calledOnDisconnect.get(), "called onDisconnect").isTrue()
    }
}