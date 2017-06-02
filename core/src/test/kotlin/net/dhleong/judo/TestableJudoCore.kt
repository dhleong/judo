package net.dhleong.judo

import javax.swing.KeyStroke

/**
 * @author dhleong
 */

class TestableJudoCore : IJudoCore {

    val echos = ArrayList<Any?>()
    val sends = ArrayList<String>()
    val maps = ArrayList<Array<Any>>()

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

    override val aliases
        get() = throw UnsupportedOperationException()

    override fun enterMode(modeName: String) {
        TODO("not implemented")
    }

    override fun exitMode() {
        TODO("not implemented")
    }

    override fun feedKey(stroke: KeyStroke, remap: Boolean) {
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
