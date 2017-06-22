package net.dhleong.judo

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.prompt.PromptManager
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
    override val triggers = TriggerManager()
    override val prompts = PromptManager()

    override fun map(mode: String, from: String, to: String, remap: Boolean) {
        maps.add(arrayOf(mode, from, to, remap))
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
    ) { _, _, _ ->
        // by default, nothing is implemented
        TODO("not implemented")
    } as IJudoCore
}
