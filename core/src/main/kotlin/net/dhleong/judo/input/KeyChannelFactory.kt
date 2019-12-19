package net.dhleong.judo.input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import net.dhleong.judo.BlockingKeySource
import java.util.concurrent.CancellationException

class CtrlCPressedException(
    val hadPendingKeys: Boolean
) : CancellationException()

class InputQueueOverflowException : CancellationException()

typealias InterruptHandler = (e: CancellationException) -> Unit

/**
 * @author dhleong
 */
interface KeyChannelFactory {
    /**
     * The returned [Channel] may be canceled with instances of
     * [CtrlCPressedException] or [InputQueueOverflowException]
     */
    fun createChannel(job: Job, onInterrupt: InterruptHandler): Channel<Key>
}

class BlockingKeySourceChannelAdapter(
    private val keySource: BlockingKeySource,
    private val interruptTimeoutMillis: Long = 350
) : KeyChannelFactory {

    override fun createChannel(job: Job, onInterrupt: InterruptHandler): Channel<Key> {
        val channel = Channel<Key>(capacity = Channel.RENDEZVOUS)

        GlobalScope.launch(Dispatchers.IO + job) {
            while (!job.isCompleted) {
                Thread.yield()

                val key = keySource.readKey() ?: continue
                if (key != Key.CTRL_C) {
                    channel.send(key)
                    continue
                }

                // ctrl-c is a special case to handle interrupts;
                select<Unit> {
                    channel.onSend(key) {
                        // sent key successfully
                    }

                    // if we are unable to send the key to the channel
                    // within some time then *something* is blocking the
                    // main thread
                    @Suppress("EXPERIMENTAL_API_USAGE")
                    onTimeout(interruptTimeoutMillis) {
                        onInterrupt(CtrlCPressedException(hadPendingKeys = true))
                    }
                }
            }

            @Suppress("EXPERIMENTAL_API_USAGE")
            if (!channel.isClosedForReceive) {
                channel.close()
            }
        }

        return channel
    }

}

fun BlockingKeySource.toChannelFactory(): KeyChannelFactory =
    BlockingKeySourceChannelAdapter(this)