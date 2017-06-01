package net.dhleong.judo.net

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import kotlin.concurrent.thread

/**
 * @author dhleong
 */

abstract class Connection : Closeable {
    abstract val input: InputStream
    abstract val output: OutputStream

    var onError: ((IOException) -> Unit)? = null
    var onDisconnect: (() -> Unit)? = null

    private val writer: Writer by lazy { BufferedWriter(OutputStreamWriter(output)) }
    private var readerThread: Thread? = null
    private var isClosed = false

    fun send(line: String) {
        writer.write(line)
        writer.write("\r\n")
        writer.flush()
    }

    fun forEachLine(onNewLine: (String) -> Unit) {
        val reader = BufferedReader(InputStreamReader(input))
        readerThread = thread {
            try {
                while (!isClosed) {
                    // FIXME actually we don't always get a full line
                    val read = reader.readLine()
                    if (read == null) {
                        isClosed = true
                        onDisconnect?.invoke()
                        break
                    }

                    onNewLine(read.trim())
                }
            } catch (e: IOException) {
                onError?.invoke(e)
            }
        }
    }
}
