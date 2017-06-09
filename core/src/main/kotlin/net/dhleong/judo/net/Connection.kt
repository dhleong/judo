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
    var onDisconnect: ((Connection) -> Unit)? = null
    var onEchoStateChanged: ((Boolean) -> Unit)? = null

    private val writer: Writer by lazy { BufferedWriter(OutputStreamWriter(output)) }
    private var readerThread: Thread? = null
    private var isClosed = false

    fun send(line: String) {
        writer.write(line)
        writer.write("\r\n")
        writer.flush()
    }

    fun forEachLine(onNewLine: (CharArray, Int) -> Unit) {
        val reader = BufferedReader(InputStreamReader(input))
        readerThread = thread {
            try {
                val buffer = CharArray(1024)
                while (!isClosed) {
                    // FIXME actually we don't always get a full line
                    val read = reader.read(buffer)
                    if (read == -1) {
                        isClosed = true
                        onDisconnect?.invoke(this)
                        break
                    } else if (read > 0) {
                        onNewLine(buffer, read)
                    }
                }
            } catch (e: IOException) {
                onError?.invoke(e)
            }
        }
    }

    abstract fun setWindowSize(width: Int, height: Int)

}
