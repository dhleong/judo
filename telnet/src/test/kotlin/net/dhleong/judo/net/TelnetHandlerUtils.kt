package net.dhleong.judo.net

import assertk.Assert
import assertk.assertAll
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import assertk.assertions.support.show
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @author dhleong
 */
fun TelnetOptionHandler.sentBytesOn(
    event: TelnetOptionHandler.(TelnetClient) -> Unit
): ByteBuffer {
    val output = ByteArrayOutputStream(1024)
    val client = TelnetClient(
        "".byteInputStream(),
        output
    )

    // apply the event
    event(client)

    return ByteBuffer.wrap(output.toByteArray()).apply {
        order(ByteOrder.BIG_ENDIAN)
    }
}

fun Assert<ByteBuffer>.hasNoMore() {
    if (!actual.hasRemaining()) return

    val remainingBytes = ByteArray(actual.remaining())
    actual.get(remainingBytes)
    expected("No more bytes to remain, but was ${show(remainingBytes)}")
}

fun Assert<ByteBuffer>.nextIsWill(telnetOpt: Byte) {
    assert(actual).nextIsCmd(TELNET_WILL, telnetOpt)
}

fun Assert<ByteBuffer>.nextIsDo(telnetOpt: Byte) {
    assert(actual).nextIsCmd(TELNET_DO, telnetOpt)
}

fun Assert<ByteBuffer>.nextIsCmd(kind: Byte, telnetOpt: Byte) {
    assertAll {
        assert(actual.get()).isEqualTo(TELNET_IAC)
        assert(actual.get(), "kind").isEqualTo(kind)
        assert(actual.get(), "opt").isEqualTo(telnetOpt)
    }
}

fun Assert<ByteBuffer>.nextIsString(string: String) {
    val nameBytes = ByteArray(string.length)
    actual.get(nameBytes, 0, string.length)
    assert(String(nameBytes)).isEqualTo(string)
}


