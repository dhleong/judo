package net.dhleong.judo.net

import java.io.*
import kotlin.concurrent.thread

/**
 * @author dhleong
 */

const val TELNET_IAC = 255.toByte()
const val TELNET_SB = 250.toByte()
const val TELNET_SE = 240.toByte()

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

internal fun isTelnetSubsequence(line: String) =
    line.length >= 3
        && line[0].toByte() == TELNET_IAC
        && line[1].toByte() == TELNET_SB
        && line.last().toByte() == TELNET_SE
