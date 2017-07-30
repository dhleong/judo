package net.dhleong.judo

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.event.EventManager
import net.dhleong.judo.event.IEventManager
import net.dhleong.judo.mapping.MapManager
import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.prompt.PromptManager
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.render.JudoTabpage
import net.dhleong.judo.render.PrimaryJudoWindow
import net.dhleong.judo.trigger.TriggerManager

/**
 * @author dhleong
 */

class TestableJudoCore : IJudoCore by Proxy<IJudoCore>() {

    private val actualEvents = EventManager()
    inner class TestableEventManager : IEventManager by actualEvents {
        override fun raise(eventName: String, data: Any?) {
            raised.add(eventName to data)
            actualEvents.raise(eventName, data)
        }
    }

    val echos = ArrayList<Any?>()
    val sends = ArrayList<String>()
    val maps = ArrayList<Array<Any>>()
    val raised = ArrayList<Pair<String, Any?>>()

    private val mapRenderer = Proxy<MapRenderer> { _, _ -> }

    override val state = StateMap()

    override val aliases = AliasManager()
    override val events = TestableEventManager()
    override val mapper = MapManager(this, state, mapRenderer)
    override val triggers = TriggerManager()
    override val prompts = PromptManager()

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


    override fun onMainThread(runnable: () -> Unit) = runnable()

    override fun send(text: String, fromMap: Boolean) {
        val processed = aliases.process(text)
        if (!processed.isEmpty()) {
            sends.add(processed.toString())
        }
    }

    override fun echo(vararg objects: Any?) {
        objects.forEach { echos.add(it) }
    }

    override fun echoRaw(vararg objects: Any?) {
        objects.forEach { echos.add(it) }
    }

    fun clearTestable() {
        echos.clear()
        sends.clear()
        maps.clear()
        raised.clear()
    }
}
