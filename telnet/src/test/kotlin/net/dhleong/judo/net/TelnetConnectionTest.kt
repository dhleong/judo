package net.dhleong.judo.net

import assertk.all
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import kotlinx.coroutines.runBlocking
import net.dhleong.judo.TestableJudoCore
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.SocketException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
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

        val reason = conn.doReadingTest {
            conn.close()
            verify(socket, times(1)).close()
        }
        assertThat(reason).isNull()
    }

    @Test(timeout = 2000) fun `Cleanly handle socket reset`() {
        val socket = mock<Closeable> {  }
        val toRead = mock<InputStream> {
            on { read(any(), any(), any()) } doAnswer {
                throw SocketException("Connection Reset")
            }
        }
        val conn = TelnetConnection(
            judo = TestableJudoCore(),
            toString = "",
            socket = socket,
            input = toRead,
            output = ByteArrayOutputStream()
        )

        val reason = conn.doReadingTest {
            // should get disconnected as a result of the exception
        }
        assertThat(reason).isNotNull().all {
            hasMessage("Connection Reset")
        }
    }

    private fun JudoConnection.doReadingTest(
        disconnectorBlock: () -> Unit
    ): IOException? {
        val calledOnDisconnect = AtomicBoolean(false)
        val disconnectReason = AtomicReference<IOException>(null)
        onDisconnect = { _, reason ->
            calledOnDisconnect.set(true)
            disconnectReason.set(reason)
        }

        val startLatch = CountDownLatch(1)
        val finishLatch = CountDownLatch(1)
        thread {
            startLatch.countDown()

            runBlocking {
                forEachLine(async = false) {
                    // ignore the line
                }
            }

            // finished reading
            finishLatch.countDown()
        }

        // wait until the thread starts reading
        startLatch.await()

        // do something to trigger a disconnect
        disconnectorBlock()

        finishLatch.await()
        assertThat(calledOnDisconnect.get(), "called onDisconnect").isTrue()

        return disconnectReason.get()
    }
}