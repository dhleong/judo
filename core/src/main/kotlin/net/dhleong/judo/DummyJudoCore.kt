package net.dhleong.judo

import net.dhleong.judo.alias.IAliasManager
import net.dhleong.judo.prompt.IPromptManager
import net.dhleong.judo.trigger.ITriggerManager
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

val DUMMY_JUDO_CORE = object : IJudoCore {
    override val aliases: IAliasManager
        get() = throw UnsupportedOperationException("not implemented")
    override val prompts: IPromptManager
        get() = throw UnsupportedOperationException("not implemented")
    override val triggers: ITriggerManager
        get() = throw UnsupportedOperationException("not implemented")
    override val state: StateMap
        get() = throw UnsupportedOperationException("not implemented")

    override fun echo(vararg objects: Any?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun enterMode(modeName: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun enterMode(mode: Mode) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun exitMode() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun connect(address: String, port: Int) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun createUserMode(name: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun disconnect() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun feedKey(stroke: KeyStroke, remap: Boolean, fromMap: Boolean) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun isConnected(): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun map(mode: String, from: String, to: String, remap: Boolean) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun map(mode: String, from: String, to: () -> Unit) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun quit() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun readKey(): KeyStroke {
        throw UnsupportedOperationException("not implemented")
    }

    override fun reconnect() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun scrollPages(count: Int) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun scrollToBottom() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun seedCompletion(text: String) {
        TODO("not implemented")
    }

    override fun send(text: String, fromMap: Boolean) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setCursorType(type: CursorType) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun unmap(mode: String, from: String) {
        throw UnsupportedOperationException("not implemented")
    }

}
