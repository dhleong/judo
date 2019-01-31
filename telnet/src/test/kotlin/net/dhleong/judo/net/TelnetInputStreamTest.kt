package net.dhleong.judo.net

import assertk.all
import assertk.assert
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.junit.Before
import org.junit.Test
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * @author dhleong
 */
class TelnetInputStreamTest {

    private lateinit var events: MutableList<TelnetEvent>

    @Before fun setUp() {
        events = mutableListOf()
    }

    @Test fun `Read normal bytes`() {
        val read = readAsString("mreynolds")
        assert(read).isEqualTo("mreynolds")
    }

    @Test fun `Dispatch DO,WILL,etc and remove from stream`() {
        val read = readAsString {
            write("mr")

            write(TELNET_IAC)
            write(TELNET_WILL)
            write(TELNET_TELOPT_MSDP)

            write("eyn")

            write(TELNET_IAC)
            write(TELNET_DO)
            write(TELNET_TELOPT_ECHO)

            write("olds")
        }
        assert(read).isEqualTo("mreynolds")

        assert(events).all {
            hasSize(2)
            containsExactly(
                TelnetEvent(byteArrayOf(TELNET_WILL, TELNET_TELOPT_MSDP)),
                TelnetEvent(byteArrayOf(TELNET_DO, TELNET_TELOPT_ECHO))
            )
        }
    }

    @Test fun `Dispatch subnegotiations`() {
        val read = readAsString {
            write("mr")

            write(TELNET_IAC)
            write(TELNET_SB)
            write(TELNET_IAC)
            write(TELNET_SE)

            write("eyn")

            write(TELNET_IAC)
            write(TELNET_SB)
            write(TELNET_TELOPT_MSDP)
            write(MSDP_VAR)
            write("name")
            write(MSDP_VAL)
            write("value")
            write(TELNET_IAC)
            write(TELNET_SE)

            write("olds")
        }
        assert(read).isEqualTo("mreynolds")

        assert(events).containsExactly(
            TelnetEvent(
                byteArrayOf(TELNET_SB, TELNET_TELOPT_MSDP, MSDP_VAR.toByte()) +
                    "name".toByteArray() +
                    byteArrayOf(MSDP_VAL.toByte()) +
                    "value".toByteArray()
            )
        )
    }

    @Test fun `Handle literal IAC`() {
        val read = readAsBytes {
            write("mreyn")

            write(TELNET_IAC)
            write(TELNET_IAC)

            write("olds")
        }
        assert(read).isEqualTo(
            "mreyn".toByteArray() +
                byteArrayOf(TELNET_IAC) +
                "olds".toByteArray()
        )
    }

    @Test fun `Handle literal IAC within SB`() {
        val read = readAsString {
            write("mreyn")

            write(TELNET_IAC)
            write(TELNET_SB)
            write(42.toByte())
            write(TELNET_IAC)
            write(TELNET_IAC)
            write(TELNET_IAC)
            write(TELNET_SE)

            write("olds")
        }
        assert(read).isEqualTo("mreynolds")

        assert(events).containsExactly(
            TelnetEvent(
                byteArrayOf(TELNET_SB, 42, TELNET_IAC)
            )
        )
    }

    @Test(timeout = 200) fun `Don't hang on incomplete read`() {
        val toRead = PipedOutputStream()
        val input = TelnetInputStream(PipedInputStream(toRead)) { /* nop */ }
        val buffer = CharArray(1024)

        toRead.write("mreynolds")

        val read = input.bufferedReader().read(buffer)
        assert(read).isEqualTo(9)
    }

    private fun readAsString(s: String): String = readAsString(s.byteInputStream())
    private fun readAsString(builder: OutputStream.() -> Unit): String =
        PipedInputStream().use { stream ->
            PipedOutputStream(stream).use(builder)

            readAsString(stream)
        }
    private fun readAsBytes(builder: OutputStream.() -> Unit): ByteArray =
        PipedInputStream().use { stream ->
            PipedOutputStream(stream).use(builder)

            readAsBytes(stream)
        }


    private fun readAsBytes(inputStream: InputStream): ByteArray =
        wrapStream(inputStream).readBytes()

    private fun readAsString(inputStream: InputStream): String =
        wrapStream(inputStream).reader().readText()

    private fun wrapStream(inputStream: InputStream) =
        TelnetInputStream(inputStream) { ev ->
            events.add(ev.copy())
        }
}


