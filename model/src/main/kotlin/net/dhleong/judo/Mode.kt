package net.dhleong.judo

import net.dhleong.judo.input.Key

/**
 * @author dhleong
 */

interface Mode {
    val name: String

    fun feedKey(key: Key, remap: Boolean = true, fromMap: Boolean = false)
    fun onEnter()

    fun onExit() {
        // nop by default
    }
}
