package net.dhleong.judo.input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import net.dhleong.judo.BlockingKeySource
import java.util.concurrent.CancellationException

class CtrlCPressedException(
    val hadPendingKeys: Boolean
) : CancellationException()

class InputQueueOverflowException : CancellationException()

/**
 * @author dhleong
 */
interface KeyChannelFactory {
    /**
     * The returned [Channel] may be canceled with instances of
     * [CtrlCPressedException] or [InputQueueOverflowException]
     */
    fun createChannel(job: Job): Channel<Key>
}

class BlockingKeySourceChannelAdapter(
    private val keySource: BlockingKeySource,
    private val queueCapacity: Int = 1024
) : KeyChannelFactory {

    override fun createChannel(job: Job): Channel<Key> {
        val channel = Channel<Key>(capacity = queueCapacity)

        GlobalScope.launch(Dispatchers.IO + job) {
            while (!job.isCompleted) {
                Thread.yield()

                val key = keySource.readKey() ?: continue
                if (key == Key.CTRL_C) {
                    channel.cancel(CtrlCPressedException(
                        hadPendingKeys = channel.poll() != null
                    ))
                    break
                }

                if (!channel.offer(key)) {
                    channel.cancel(InputQueueOverflowException())
                    break
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