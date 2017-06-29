package net.dhleong.judo

import javax.swing.KeyStroke

/**
 * @author dhleong
 */

interface BlockingKeySource {
    fun readKey(): KeyStroke?
}
