package net.dhleong.judo

import kotlinx.coroutines.CoroutineDispatcher
import net.dhleong.judo.alias.IAliasManager
import net.dhleong.judo.event.IEventManager
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.Key
import net.dhleong.judo.logging.ILogManager
import net.dhleong.judo.mapping.IMapManager
import net.dhleong.judo.net.JudoConnection
import net.dhleong.judo.prompt.IPromptManager
import net.dhleong.judo.register.IRegisterManager
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.script.IJudoScrollable
import net.dhleong.judo.trigger.IMultiTriggerManager
import net.dhleong.judo.trigger.ITriggerManager
import java.io.File
import java.net.URI

val KEY_PERSIST_INPUT_HISTORY_PATH = StateKind<File>("net.dhleong.judo.persistentInput")

typealias OperatorFunc = suspend (IntRange) -> Unit

/**
 * @author dhleong
 */
interface IJudoCore : IJudoScrollable {
    val dispatcher: CoroutineDispatcher

    val aliases: IAliasManager
    val connection: JudoConnection?
    val events: IEventManager
    val logging: ILogManager
    val mapper: IMapManager
    val prompts: IPromptManager
    val registers: IRegisterManager
    val triggers: ITriggerManager
    val multiTriggers: IMultiTriggerManager
    val state: StateMap
    val renderer: JudoRenderer

    val tabpage: IJudoTabpage
    val primaryWindow: IJudoWindow

    fun echo(vararg objects: Any?)
    fun print(vararg objects: Any?)
    /** not processed for triggers, etc. */
    fun printRaw(vararg objects: Any?)
    fun enterMode(modeName: String)
    fun enterMode(mode: Mode)
    fun exitMode()
    fun connect(uri: URI)
    fun createUserMode(name: String)
    fun disconnect()
    suspend fun feedKey(stroke: Key, remap: Boolean = true, fromMap: Boolean = false)
    suspend fun feedKeys(keys: String, remap: Boolean = true, mode: String = "")
    suspend fun feedKeys(keys: Sequence<Key>, remap: Boolean = true, mode: String = "")
    fun isConnected(): Boolean
    fun map(mode: String, from: String, to: String, remap: Boolean)
    fun map(mode: String, from: String, to: suspend () -> Unit, description: String = "")
    fun onMainThread(runnable: suspend () -> Unit)
    fun persistInput(file: File)
    fun printMappings(mode: String)
    fun quit()
    suspend fun readCommandLineInput(
        prefix: Char,
        history: IInputHistory,
        bufferContents: String = ""
    ): String?
    suspend fun readKey(): Key
    fun reconnect()
    fun redraw()
    fun searchForKeyword(text: CharSequence, direction: Int = 1)
    fun seedCompletion(text: String)
    fun send(text: String, fromMap: Boolean)
    /** Called from an input mode when a line of text is "submitted" */
    fun submit(text: String, fromMap: Boolean)
    fun setCursorType(type: CursorType)
    fun unmap(mode: String, from: String)
}

