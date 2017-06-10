package net.dhleong.judo

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.prompt.PromptManager
import net.dhleong.judo.trigger.TriggerManager
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

class TestableJudoCore : IJudoCore {
    override fun isConnected(): Boolean {
        TODO("not implemented")
    }

    override fun reconnect() {
        TODO("not implemented")
    }

    override fun createUserMode(name: String) {
        TODO("not implemented")
    }

    override fun map(mode: String, from: String, to: () -> Unit) {
        TODO("not implemented")
    }

    override fun setCursorType(type: CursorType) {
        TODO("not implemented")
    }

    val echos = ArrayList<Any?>()
    val sends = ArrayList<String>()
    val maps = ArrayList<Array<Any>>()

    override val aliases = AliasManager()
    override val triggers = TriggerManager()
    override val prompts = PromptManager()
    override var opfunc: OperatorFunc? = null

    override fun readKey(): KeyStroke {
        TODO("not implemented")
    }

    override fun scrollToBottom() {
        TODO("not implemented")
    }

    override fun scrollPages(count: Int) {
        TODO("not implemented")
    }

    override fun connect(address: String, port: Int) {
        TODO("not implemented")
    }

    override fun disconnect() {
        TODO("not implemented")
    }

    override fun quit() {
        TODO("not implemented")
    }

    override fun enterMode(modeName: String) {
        TODO("not implemented")
    }

    override fun exitMode() {
        TODO("not implemented")
    }

    override fun feedKey(stroke: KeyStroke, remap: Boolean, fromMap: Boolean) {
        TODO("not implemented")
    }

    override fun map(mode: String, from: String, to: String, remap: Boolean) {
        maps.add(arrayOf(mode, from, to, remap))
    }

    override fun send(text: String, fromMap: Boolean) {
        sends.add(text)
    }

    override fun echo(vararg objects: Any?) {
        objects.forEach { echos.add(it) }
    }

    fun clearTestable() {
        echos.clear()
        sends.clear()
        maps.clear()
    }
}
