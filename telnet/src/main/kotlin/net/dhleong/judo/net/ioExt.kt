package net.dhleong.judo.net

fun OutputStream.write(byte: Byte) = write(byte.toInt() and 0xff)

/**
 * Write a string to the OutputStream
 *
 * @author dhleong
 */
fun OutputStream.write(s: String) {
    // creating a throwaway writer like this is a bit wasteful...
    // we could probably refactor to have a special OutputStream
    // subclass that reuses a Writer
    writer().apply {
        write(s)
        flush()
    }
}

fun OutputStream.writeShort(short: Int) {
    writeMaybeIac(((short shr 8) and 0xff).toByte())
    writeMaybeIac((short and 0xff).toByte())
}

fun OutputStream.writeMaybeIac(byte: Byte) {
    if (byte == TELNET_IAC) {
        // duplicate it
        write(byte)
    }
    write(byte)
}
