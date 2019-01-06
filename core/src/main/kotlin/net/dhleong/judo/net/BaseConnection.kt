package net.dhleong.judo.net

import java.io.BufferedReader
import java.io.BufferedWriter
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
abstract class BaseConnection : JudoConnection {
    /** in ms */
    val DEFAULT_CONNECT_TIMEOUT = 20000

    abstract val input: InputStream
    abstract val output: OutputStream

    override var onError: ((IOException) -> Unit)? = null
    override var onDisconnect: ((JudoConnection) -> Unit)? = null
    override var onEchoStateChanged: ((Boolean) -> Unit)? = null

    private val writer: Writer by lazy { BufferedWriter(OutputStreamWriter(output)) }
    private var readerThread: Thread? = null
    protected var isClosed = false

    override fun send(line: String) {
        writer.write(line)
        writer.write("\r\n")
        writer.flush()
    }

    override fun forEachLine(onNewLine: (CharArray, Int) -> Unit) {
        val reader = BufferedReader(InputStreamReader(input))
        readerThread = thread(isDaemon = true) {
            try {
                val buffer = CharArray(1024)
                while (!isClosed) {
                    reader.ready()
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
}

