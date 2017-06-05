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
    fun exitMode()
    fun connect(address: String, port: Int)
    fun disconnect()
    fun feedKey(stroke: KeyStroke, remap: Boolean = true)
    fun map(mode: String, from: String, to: String, remap: Boolean)
    fun readKey(): KeyStroke
    fun scrollPages(count: Int)
    fun scrollToBottom()
    fun send(text: String, fromMap: Boolean)
    fun quit()
}

