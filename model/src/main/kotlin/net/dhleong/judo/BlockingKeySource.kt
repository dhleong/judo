package net.dhleong.judo

import net.dhleong.judo.input.Key

/**
 * @author dhleong
 */

interface BlockingKeySource {
    fun readKey(): Key?
}
