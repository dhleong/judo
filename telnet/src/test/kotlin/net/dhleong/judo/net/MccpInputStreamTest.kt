package net.dhleong.judo.net

import assertk.Assert
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import net.dhleong.judo.net.options.MccpInputStream
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * @author dhleong
 */
class MccpInputStreamTest {

    @Test fun `Enter compressed mode`() {
        val input = "ÿùÿü\u0019ÿüÈÿüÉÿúVÿðhCúÿK\u0082ñÿ\u0007\u0000\u0000\u0000\u0000ÿÿ".map {
            it.toInt().toByte()
        }.toByteArray()
        val stream = MccpInputStream(ByteArrayInputStream(input))

        val result = ByteArray(1024)
        var readCount = stream.read(result)
        var read = result.take(readCount).map { it.toUInt() }

        assertThat(read).endsWith(
            TELNET_IAC.toUInt(),
            TELNET_SB.toUInt(),
            TELNET_TELOPT_MCCP2.toUInt(),
            TELNET_IAC.toUInt(),
            TELNET_SE.toUInt()
        )
        assertThat(stream.compressEnabled).isTrue()

        readCount = stream.read(result)
        read = result.take(readCount).map { it.toUInt() }

        assertThat(read).containsExactly(
            TELNET_IAC.toUInt(),
            TELNET_SB.toUInt(),
            TELNET_TELOPT_TERMINAL_TYPE.toUInt(),
            1,
            TELNET_IAC.toUInt(),
            TELNET_SE.toUInt()
        )
    }

    @Test fun `Less output than input`() {
        val input = "ÿùÿü\u0019ÿüÈÿüÉÿúVÿðhCúÿK\u0082ñÿ\u0007\u0000\u0000\u0000\u0000ÿÿ".map {
            it.toInt().toByte()
        }.toByteArray()
        val stream = MccpInputStream(ByteArrayInputStream(input))

        val result = ByteArray(1024)
        stream.read(result)

        // read single bytes to test proper handling of having more input
        // we have room for the output
        assertThat(stream.readByte()).isEqualTo(TELNET_IAC)
        assertThat(stream.readByte()).isEqualTo(TELNET_SB)
        assertThat(stream.readByte()).isEqualTo(TELNET_TELOPT_TERMINAL_TYPE)
        assertThat(stream.readByte()).isEqualTo(1)
        assertThat(stream.readByte()).isEqualTo(TELNET_IAC)
        assertThat(stream.readByte()).isEqualTo(TELNET_SE)

        assertThat(stream.read(result)).isEqualTo(-1)
    }

}

private fun <T> Assert<List<T>>.endsWith(
    vararg entries: T
) = given { actual ->
    assertThat(actual)
        .transform("last ${entries.size} entries") { it.takeLast(entries.size) }
        .containsExactly(*entries)
}

private fun Byte.toUInt(): Int = toInt() and 0xff

private fun MccpInputStream.readByte(): Byte {
    val singleByte = ByteArray(1)
    val read = read(singleByte)
    assertThat(read, "read byte count")
        .isEqualTo(1)
    return singleByte[0]
}

