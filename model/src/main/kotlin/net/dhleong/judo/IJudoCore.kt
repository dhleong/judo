package net.dhleong.judo

import javax.swing.KeyStroke

/**
 * @author dhleong
 */
interface IJudoCore {

    val aliases: IAliasManager

    fun echo(vararg objects: Any?)
    fun enterMode(modeName: String)
    fun connect(address: String, port: Int)
    fun disconnect()
    fun feedKey(stroke: KeyStroke, remap: Boolean = true)
    fun map(mode: String, from: String, to: String, remap: Boolean)
    fun scrollPages(count: Int)
    fun scrollToBottom()
    fun send(text: String)
    fun quit()
}

