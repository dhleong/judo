package net.dhleong.judo

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import net.dhleong.judo.input.InterruptHandler
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyChannelFactory
import net.dhleong.judo.input.Keys
import net.dhleong.judo.util.asChannel

/**
 * @author dhleong
 */

fun JudoCore.setInput(buffer: String, cursor: Int) {
    this.buffer.set(buffer)
    this.buffer.cursor = cursor
}

fun JudoCore.type(keys: Keys) {
    readKeys(object : KeyChannelFactory {
        override fun createChannel(job: Job, onInterrupt: InterruptHandler): Channel<Key> =
            keys.asChannel()
    })
}

