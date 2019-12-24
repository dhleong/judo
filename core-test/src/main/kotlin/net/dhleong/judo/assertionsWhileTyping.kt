package net.dhleong.judo

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dhleong.judo.input.InterruptHandler
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyChannelFactory
import net.dhleong.judo.input.Keys
import java.util.concurrent.atomic.AtomicReference

class AssertionContext(
    internal val judo: JudoCore,
    internal val channel: Channel<Key>
)

/**
 * @author dhleong
 */
inline fun assertionsWhileTyping(
    judo: JudoCore,
    crossinline block: suspend AssertionContext.() -> Unit
) {
    // NOTE: we have to catch any exceptions (including from
    // assertions) and re-throw them later, since feedKeys
    // normally consumes exceptions and prints them to the buffer
    val error = AtomicReference<Throwable>(null)
    val channel = Channel<Key>()
    val context = AssertionContext(judo, channel)

    GlobalScope.launch {
        try {
            context.block()
        } catch (e: Throwable) {
            error.set(e)
        } finally {
            channel.close()
        }
    }

    judo.readKeys(object : KeyChannelFactory {
        override fun createChannel(job: Job, onInterrupt: InterruptHandler): Channel<Key> = channel
    })

    error.get()?.let { throw it }
}

suspend fun AssertionContext.yieldKeys(keys: String) {
    for (k in Keys.parse(keys)) {
        channel.send(k)

        // delay to ensure JudoCore has sufficient time to asynchronously
        // process the key across suspend points.
        // FIXME this is terrible. there has got to be a better way to do do this...
        delay(10)
    }
}
