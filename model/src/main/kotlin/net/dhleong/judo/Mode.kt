package net.dhleong.judo

import javax.swing.KeyStroke

/**
 * @author dhleong
 */

interface Mode {
    val name: String

    fun feedKey(key: KeyStroke, remap: Boolean = true, fromMap: Boolean = false)
    fun onEnter()
}
