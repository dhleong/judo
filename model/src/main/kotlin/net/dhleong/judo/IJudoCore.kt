package net.dhleong.judo

import net.dhleong.judo.alias.IAliasManager
import net.dhleong.judo.prompt.IPromptManager
import net.dhleong.judo.trigger.ITriggerManager
import javax.swing.KeyStroke

typealias OperatorFunc = (IntRange) -> Unit

/**
 * @author dhleong
 */
interface IJudoCore {

    val aliases: IAliasManager
    val prompts: IPromptManager
    val triggers: ITriggerManager

    var opfunc: OperatorFunc?

    fun echo(vararg objects: Any?)
    fun enterMode(modeName: String)
    fun enterMode(mode: Mode)
    fun exitMode()
    fun connect(address: String, port: Int)
    fun createUserMode(name: String)
    fun disconnect()
    fun feedKey(stroke: KeyStroke, remap: Boolean = true, fromMap: Boolean = false)
    fun isConnected(): Boolean
    fun map(mode: String, from: String, to: String, remap: Boolean)
    fun map(mode: String, from: String, to: () -> Unit)
    fun readKey(): KeyStroke
    fun reconnect()
    fun scrollPages(count: Int)
    fun scrollToBottom()
    fun send(text: String, fromMap: Boolean)
    fun setCursorType(type: CursorType)
    fun quit()
}

