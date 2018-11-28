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

const val TELNET_IAC = 255.toByte()
const val TELNET_SB = 250.toByte()
const val TELNET_SE = 240.toByte()

abstract class Connection : JudoConnection {
    /** in ms */
    val DEFAULT_CONNECT_TIMEOUT = 20000

    abstract val input: InputStream
    abstract val output: OutputStream

    var onError: ((IOException) -> Unit)? = null
    var onDisconnect: ((Connection) -> Unit)? = null
    var onEchoStateChanged: ((Boolean) -> Unit)? = null

    private val writer: Writer by lazy { BufferedWriter(OutputStreamWriter(output)) }
    private var readerThread: Thread? = null
    protected var isClosed = false

    open fun send(line: String) {
        writer.write(line)
        writer.write("\r\n")
        writer.flush()
    }

    fun forEachLine(onNewLine: (CharArray, Int) -> Unit) {
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

    abstract fun setWindowSize(width: Int, height: Int)

    internal fun isTelnetSubsequence(line: String) =
        line.length >= 3
            && line[0].toByte() == TELNET_IAC
            && line[1].toByte() == TELNET_SB
            && line.last().toByte() == TELNET_SE
}
