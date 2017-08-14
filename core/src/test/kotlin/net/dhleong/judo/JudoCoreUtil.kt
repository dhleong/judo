package net.dhleong.judo

import net.dhleong.judo.input.Key
import net.dhleong.judo.input.Keys

/**
 * @author dhleong
 */

fun JudoCore.setInput(buffer: String, cursor: Int) {
    this.buffer.set(buffer)
    this.buffer.cursor = cursor
}

fun JudoCore.type(keys: Keys) {
    val keysIter = keys.iterator()
    readKeys(object : BlockingKeySource {
        override fun readKey(): Key {
            val next = keysIter.next()
            if (!keysIter.hasNext()) {
                running = false
            }
            return next
        }
    })

    running = true // restore
}

