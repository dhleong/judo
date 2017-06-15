package net.dhleong.judo

import net.dhleong.judo.alias.IAliasManager
import net.dhleong.judo.prompt.IPromptManager
import net.dhleong.judo.trigger.ITriggerManager
import javax.swing.KeyStroke

typealias OperatorFunc = (IntRange) -> Unit

@Suppress("unused")
data class StateKind<E>(val name: String)

/**
 * StateMap provides typesafe storage for whatever intermediate
 * state a motion or mode, etc. might need for communicating
 * with other motions or modes. The ;/, motions, for example,
 * need to know how to repeat the last f/F/t/T, and store that
 * in the StateMap.
 */
@Suppress("UNCHECKED_CAST")
class StateMap {
    private val map = HashMap<StateKind<*>, Any>()

    operator fun <E : Any> set(key: StateKind<E>, value: E) {
        map[key] = value
    }

    operator fun <E : Any> get(key: StateKind<E>): E? =
        map[key] as E?

    operator fun <E : Any> contains(key: StateKind<E>): Boolean =
        key in map

    fun <E : Any> remove(key: StateKind<E>): E? =
        map.remove(key) as E?
}

/**
 * @author dhleong
 */
interface IJudoCore {

    val aliases: IAliasManager
    val prompts: IPromptManager
    val triggers: ITriggerManager
    val state: StateMap

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
    fun quit()
    fun readKey(): KeyStroke
    fun reconnect()
    fun scrollPages(count: Int)
    fun scrollToBottom()
    fun send(text: String, fromMap: Boolean)
    fun setCursorType(type: CursorType)
    fun unmap(mode: String, from: String)
}

