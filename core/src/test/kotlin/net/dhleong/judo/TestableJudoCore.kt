package net.dhleong.judo

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.event.EventManager
import net.dhleong.judo.prompt.PromptManager
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.render.JudoTabpage
import net.dhleong.judo.render.PrimaryJudoWindow
import net.dhleong.judo.trigger.TriggerManager
import java.lang.reflect.Proxy

/**
 * @author dhleong
 */

class TestableJudoCore : IJudoCore by createCoreProxy() {

    val echos = ArrayList<Any?>()
    val sends = ArrayList<String>()
    val maps = ArrayList<Array<Any>>()

    override val aliases = AliasManager()
    override val events = EventManager()
    override val triggers = TriggerManager()
    override val prompts = PromptManager()
    override val state = StateMap()

    val ids = IdManager()
    override var tabpage: IJudoTabpage = JudoTabpage(
        ids, state,
        PrimaryJudoWindow(ids, state,
            JudoBuffer(ids),
            42, 24
        )
    )

    override fun map(mode: String, from: String, to: String, remap: Boolean) {
        maps.add(arrayOf(mode, from, to, remap))
    }

    override fun unmap(mode: String, from: String) {
        maps.removeIf { it[0] == mode && it[1] == from }
    }

    override fun send(text: String, fromMap: Boolean) {
        val processed = aliases.process(text)
        if (!processed.isEmpty()) {
            sends.add(processed.toString())
        }
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

private fun createCoreProxy(): IJudoCore {
    return Proxy.newProxyInstance(
        ClassLoader.getSystemClassLoader(),
        arrayOf(IJudoCore::class.java)
    ) { _, method, _ ->
        // by default, nothing is implemented
        throw UnsupportedOperationException(
            "${method.name} is not implemented")
    } as IJudoCore
}
