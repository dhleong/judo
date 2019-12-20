package net.dhleong.judo.util

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Semaphore

/**
 * @author dhleong
 */
class FlowIterator<T>(
    private val flow: Flow<T>
) : Iterator<T> {

    private val pending = Semaphore(0)

    @Suppress("EXPERIMENTAL_API_USAGE")
    private val channel by lazy {
        Channel<T>().also { ch ->
            GlobalScope.launch {
                flow.onCompletion {
                    println("onCompletion")
                    pending.release()
                    ch.close()
                }.collect {
                    pending.release()
                    ch.send(it)
                }
            }
            println("Wait for flow to warm")
            pending.acquire()
            println("flow started")
            // give it back
            pending.release()
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    override fun hasNext(): Boolean {
        if (channel.isClosedForReceive) {
            // channel is already closed
            return false
        }

        // wait for some signal
        pending.acquire()

        // now we know for sure
        return !channel.isClosedForReceive
    }

    override fun next(): T = runBlocking {
        channel.receive()
    }

}

fun <T> Flow<T>.asIterator() = FlowIterator(this)

fun <T> Sequence<T>.asChannel() = iterator().asChannel()
fun <T> Iterable<T>.asChannel() = iterator().asChannel()
fun <T> Iterator<T>.asChannel() = Channel<T>().apply {
    GlobalScope.launch {
        while (hasNext()) {
            send(next())
        }
        close()
    }
}

fun <T> Channel<T>.asSequence(): Sequence<T> {
    val ch = this
    return sequence<T> {
        @Suppress("EXPERIMENTAL_API_USAGE")
        while (!ch.isClosedForReceive) {
            val v = ch.poll()
            if (v != null) {
                yield(v)
            }
        }
    }
}