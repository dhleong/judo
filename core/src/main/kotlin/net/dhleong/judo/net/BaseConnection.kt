package net.dhleong.judo.net

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    override var onDisconnect: ((JudoConnection, reason: IOException?) -> Unit)? = null
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

    override fun forEachLine(
        async: Boolean,
        onNewLine: (FlavorableCharSequence) -> Unit
    ) {
        startTasks()

        maybeAsync(async) {
            while (job.isActive) {
                @Suppress("EXPERIMENTAL_API_USAGE")
                val read = readTask.receiveOrNull()
                    ?: break // disconnected

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

        // ensure we notify
        notifyDisconnect()
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun startTasks() {
        writeTask.start()

        @ExperimentalCoroutinesApi
        readTask = produce(Dispatchers.IO) {
            val buffer = CharArray(1024)
            val reader = input.bufferedReader()
            val helper = AnsiFlavorableStringReader()
            var reason: IOException? = null
            while (job.isActive) {
                val read = try {
                    reader.read(buffer)
                } catch (e: IOException) {
                    reason = e
                    -1
                }
                if (read == -1) {
                    notifyDisconnect(reason)
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

    private fun notifyDisconnect(reason: IOException? = null) = synchronized(this) {
        // there can be only one
        val callback = onDisconnect
        onDisconnect = null

        callback?.invoke(this, reason)
    }

    private inline fun maybeAsync(async: Boolean, crossinline block: suspend CoroutineScope.() -> Unit) {
        if (async) {
            launch(Dispatchers.Default + job) {
                block()
            }
        } else {
            try {
                runBlocking(job) {
                    block()
                }
            } catch (e: CancellationException) {
                // it's okay
            }
        }
    }
}
