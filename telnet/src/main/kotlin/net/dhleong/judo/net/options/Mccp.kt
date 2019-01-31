package net.dhleong.judo.net.options

import net.dhleong.judo.net.TELNET_IAC
import net.dhleong.judo.net.TELNET_SB
import net.dhleong.judo.net.TELNET_TELOPT_MCCP2
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream
import java.util.zip.Inflater

class MccpInputStream(
    delegate: InputStream,
    private val debug: (String) -> Unit = { /* nop */ }
): InputStream() {

    var compressEnabled = false
    private val inputStream = PushbackInputStream(delegate, 8192)
    private val inflater = Inflater()
    private val buffer = ByteArray(1024)

    override fun read(): Int = throw UnsupportedOperationException("Use read(byte[])")

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (compressEnabled) {
            if (!inflater.finished()) {
                return inflateInto(b, off, len)
            }

            // server terminated compression
            compressEnabled = false
            debug("## MCCP Disabled")
        }

        // enable mccp?
        var read = inputStream.read(b, off, len)

        for (i in 0..(read - 4)) {
            if (b[i] != TELNET_IAC) continue

            // mccp?
            if (b[i + 1].toInt() == TELNET_SB.toInt() && b[i + 2] == TELNET_TELOPT_MCCP2) {
                compressEnabled = true
                debug("## MCCP Enabled")

                val afterSubEnd = i + 5 // 3 = IAC; 4 = SE
                val left = read - afterSubEnd
                if (left > 0) {
                    read = afterSubEnd
                    inputStream.unread(b, afterSubEnd, left)
                }
                break
            }
        }
        return read

    }

    private fun inflateInto(b: ByteArray, off: Int, len: Int): Int {
        val read = inputStream.read(buffer)
        if (read == -1) return -1

        inflater.setInput(buffer, 0, read)

        var inflated = 0
        var offset = off
        var available = len
        while (inflater.remaining > 0 && available > 0) {
            val readBytes = inflater.inflate(b, offset, available)
            inflated += readBytes
            available -= readBytes
            offset += readBytes

            if (readBytes == 0) {
                if (inflater.finished()) break
                if (inflater.needsDictionary()) {
                    return -1
                }
            }
        }

        if (inflater.remaining > 0) {
            val start = read - inflater.remaining
            try {
                inputStream.unread(buffer, start, inflater.remaining)
            } catch (e: IOException) {
                throw IOException("Unable to unread ${inflater.remaining} bytes", e)
            }
        }

        return inflated
    }
}
