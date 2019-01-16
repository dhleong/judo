package net.dhleong.judo.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import net.dhleong.judo.render.FlavorableCharSequence
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext

/**
 * @author dhleong
 */
abstract class BaseConnection(
    debug: Boolean
) : JudoConnection, CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    protected abstract val input: InputStream
    protected abstract val output: OutputStream

    override var onError: ((IOException) -> Unit)? = null
    override var onDisconnect: ((JudoConnection) -> Unit)? = null
    override var onEchoStateChanged: ((Boolean) -> Unit)? = null

    private val writeQueue = Channel<String>(capacity = 8)
    private val writeTask = launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
        val writer = output.writer()
        for (packet in writeQueue) {
            writer.apply {
                write(packet)
                write("\r\n")
                flush()
            }
        }
    }

    private lateinit var readTask: ReceiveChannel<FlavorableCharSequence>

    private val debugStream = if (debug) {
        File("net-debug.txt")
            .outputStream()
            .bufferedWriter()
    } else null

    override suspend fun send(line: String) {
        writeQueue.send(line)
    }

    override fun forEachLine(onNewLine: (FlavorableCharSequence) -> Unit) {
        startTasks()

        launch(Dispatchers.Default + job) {
            while (job.isActive) {
                @Suppress("EXPERIMENTAL_API_USAGE")
                val read = readTask.receiveOrNull()
                    ?: return@launch // disconnected

                onNewLine(read)
            }
        }
    }

    override fun close() {
        debugStream?.apply {
            flush()
            close()
        }
        job.cancel()
        writeTask.cancel()
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun startTasks() {
        writeTask.start()

        @ExperimentalCoroutinesApi
        readTask = produce(Dispatchers.IO) {
            val buffer = CharArray(1024)
            val reader = input.bufferedReader()
            val helper = AnsiFlavorableStringReader()
            while (job.isActive) {
                val read = reader.read(buffer)
                if (read == -1) {
                    onDisconnect?.invoke(this@BaseConnection)
                    break
                } else if (read > 0) {

                    debugStream?.apply {
                        write(buffer, 0, read)
                        flush()
                    }

                    for (line in helper.feed(buffer, available = read)) {
                        send(line)
                    }
                }
            }
        }
    }

}