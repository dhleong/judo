package net.dhleong.judo

import net.dhleong.judo.input.Keys
import javax.swing.KeyStroke

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
        override fun readKey(): KeyStroke {
            val next = keysIter.next()
            if (!keysIter.hasNext()) {
                running = false
            }
            return next
        }
    })

    running = true // restore
}

