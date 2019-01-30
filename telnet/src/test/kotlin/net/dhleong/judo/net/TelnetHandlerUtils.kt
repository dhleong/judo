package net.dhleong.judo.net

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