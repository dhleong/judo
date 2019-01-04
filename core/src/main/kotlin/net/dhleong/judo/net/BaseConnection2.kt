package net.dhleong.judo.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import net.dhleong.judo.render.FlavorableCharSequence
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext

/**
 * @author dhleong
 */
abstract class BaseConnection2 : JudoConnection2, CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    protected abstract val input: InputStream
    protected abstract val output: OutputStream

    override var onError: ((IOException) -> Unit)? = null
    override var onDisconnect: ((JudoConnection2) -> Unit)? = null
    override var onEchoStateChanged: ((Boolean) -> Unit)? = null

    private val writeQueue = Channel<String>(capacity = 8)
    private val writeTask = launch(Dispatchers.IO) {
        val writer = output.writer()
        for (packet in writeQueue) {
            writer.apply {
                write(packet)
                write("\r\n")
                flush()
            }
        }
    }

    private val readTask = produce(Dispatchers.IO) {
        val buffer = CharArray(1024)
        val reader = input.bufferedReader()
        val helper = AnsiFlavorableStringReader()
        while (job.isActive) {
            val read = reader.read(buffer)
            if (read == -1) {
                onDisconnect?.invoke(this@BaseConnection2)
                break
            } else if (read > 0) {
                for (line in helper.feed(buffer, read)) {
                    send(line)
                }
            }
        }
    }

    override suspend fun send(line: String) {
        writeQueue.send(line)
    }

    override fun forEachLine(onNewLine: (FlavorableCharSequence) -> Unit) {
        launch(Dispatchers.Main) {
            val read = readTask.receive()
            onNewLine(read)
        }
    }

    override fun close() {
        job.cancel()
        writeTask.cancel()
    }

}