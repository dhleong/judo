package net.dhleong.judo.net

import net.dhleong.judo.assertThat
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * @author dhleong
 */
class MccpInputStreamTest {

    lateinit var mccp: MccpHandlingSocketFactory
    lateinit var socket: MccpHandlingSocket

    @Before fun setUp() {
        mccp = MccpHandlingSocketFactory { System.out.println(it) }
        socket = mccp.createSocket() as MccpHandlingSocket
    }

    @Test fun enterCompressedMode() {
        val input = "ÿùÿü\u0019ÿüÈÿüÉÿúVÿðhCúÿK\u0082ñÿ\u0007\u0000\u0000\u0000\u0000ÿÿ".map {
            it.toInt().toByte()
        }.toByteArray()
        val stream = MccpInputStream(socket, ByteArrayInputStream(input))

        val result = ByteArray(1024)
        var readCount = stream.read(result)
        var read = result.take(readCount).map { it.toUInt() }

        assertThat(read).endsWith(
            TELNET_IAC.toUInt(),
            TELNET_SB.toUInt(),
            TELNET_TELOPT_MCCP2.toUInt(),
            TELNET_IAC.toUInt(),
            TELNET_SE.toUInt())
        assertThat(socket.compressEnabled).isTrue()

        readCount = stream.read(result)
        read = result.take(readCount).map { it.toUInt() }

        assertThat(read).containsExactly(
            TELNET_IAC.toUInt(),
            TELNET_SB.toUInt(),
            TELNET_TELOPT_TERMINAL_TYPE.toUInt(),
            TELNET_DO.toUInt(),
            TELNET_IAC.toUInt(),
            TELNET_SE.toUInt())
    }

    @Test fun lessOutputThanInput() {
        val input = "ÿùÿü\u0019ÿüÈÿüÉÿúVÿðhCúÿK\u0082ñÿ\u0007\u0000\u0000\u0000\u0000ÿÿ".map {
            it.toInt().toByte()
        }.toByteArray()
        val stream = MccpInputStream(socket, ByteArrayInputStream(input))

        val result = ByteArray(1024)
        stream.read(result)

        // read single bytes to test proper handling of having more input
        // we have room for the output
        assertThat(stream.readByte()).isEqualTo(TELNET_IAC)
        assertThat(stream.readByte()).isEqualTo(TELNET_SB)
        assertThat(stream.readByte()).isEqualTo(TELNET_TELOPT_TERMINAL_TYPE)
        assertThat(stream.readByte()).isEqualTo(TELNET_DO)
        assertThat(stream.readByte()).isEqualTo(TELNET_IAC)
        assertThat(stream.readByte()).isEqualTo(TELNET_SE)

        assertThat(stream.read(result)).isEqualTo(-1)
    }

}

private fun Byte.toUInt(): Int = toInt() and 0xff

private fun MccpInputStream.readByte(): Byte {
    val singleByte = ByteArray(1)
    val read = read(singleByte)
    assertThat(read)
        .overridingErrorMessage("Expected to read exactly one byte, but read $read")
        .isEqualTo(1)
    return singleByte[0]
}

