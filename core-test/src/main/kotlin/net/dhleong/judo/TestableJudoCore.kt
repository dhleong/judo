package net.dhleong.judo

import com.nhaarman.mockito_kotlin.mock
import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.event.EventManager
import net.dhleong.judo.event.IEventManager
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.Keys
import net.dhleong.judo.input.action
import net.dhleong.judo.mapping.MapManager
import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.modes.NormalMode
import net.dhleong.judo.modes.OperatorPendingMode
import net.dhleong.judo.prompt.PromptManager
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.trigger.TriggerManager
import net.dhleong.judo.util.InputHistory

/**
 * @author dhleong
 */

class TestableJudoCore(
    override val renderer: JudoRenderer = mock {  }
) : IJudoCore by Proxy() {

    private val actualEvents = EventManager()
    inner class TestableEventManager : IEventManager by actualEvents {
        override fun raise(eventName: String, data: Any?) {
            raised.add(eventName to data)
            actualEvents.raise(eventName, data)
        }
    }

    val prints = ArrayList<Any?>()
    val sends = ArrayList<String>()
    val maps = ArrayList<List<Any>>()
    val raised = ArrayList<Pair<String, Any?>>()

    private val mapRenderer = Proxy<MapRenderer> { _, _ -> }

    override val state = StateMap()

    override val aliases = AliasManager()
    override val events = TestableEventManager()
    override val mapper = MapManager(this, state, mapRenderer)
    override val triggers = TriggerManager()
    override val prompts = PromptManager()

    val ids = IdManager()
    override val tabpage: IJudoTabpage = createTabpageMock(ids)

    private val buffer = InputBuffer()
    private val normalMode = NormalMode(
        this, buffer, InputHistory(buffer),
        OperatorPendingMode(this, buffer)
    )

    override fun map(mode: String, from: String, to: String, remap: Boolean) {
        maps.add(listOf(mode, from, to, remap))
        if (mode == normalMode.name) {
            normalMode.userMappings.map(Keys.parse(from), Keys.parse(to))
        }
    }

    override fun map(mode: String, from: String, to: () -> Unit, description: String) {
        maps.add(listOf(mode, from, to, false))
        if (mode == normalMode.name) {
            normalMode.userMappings.map(Keys.parse(from), action(to))
        }
    }

    override fun feedKey(stroke: Key, remap: Boolean, fromMap: Boolean) {
        normalMode.feedKey(stroke, remap, fromMap)
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

    override fun print(vararg objects: Any?) {
        objects.forEach { prints.add(it) }
    }

    override fun printRaw(vararg objects: Any?) {
        objects.forEach { prints.add(it) }
    }

    fun clearTestable() {
        prints.clear()
        sends.clear()
        maps.clear()
        raised.clear()
    }
}

private fun createTabpageMock(ids: IdManager) = object : IJudoTabpage by Proxy() {
    private val windows = mutableMapOf<Int, IJudoWindow>()
    override val id: Int = ids.newTabpage()
    override fun hsplit(rows: Int, buffer: IJudoBuffer): IJudoWindow =
        createWindowMock(ids, rows, buffer).also {
            windows[it.id] = it
        }

    override fun findWindowById(id: Int): IJudoWindow? = windows[id]

    override var currentWindow: IJudoWindow
        get() = windows.values.first()
        set(_) { TODO("not implemented") }
}

fun createWindowMock(
    ids: IdManager,
    rows: Int,
    buffer: IJudoBuffer,
    isFocusable: Boolean = true
) = object : IJudoWindow by DumbProxy() {

    override val id = ids.newWindow()

    override var width = 0
    override var height = rows

    override var currentBuffer: IJudoBuffer
        get() = buffer
        set(_) { TODO("not implemented") }

    override val isFocusable: Boolean = isFocusable
    override var isFocused: Boolean = isFocusable

    override fun append(text: FlavorableCharSequence) = buffer.append(text)
    override fun appendLine(line: FlavorableCharSequence) = buffer.appendLine(line)
    override fun appendLine(line: String) =
        buffer.appendLine(FlavorableStringBuilder.withDefaultFlavor(line))

    override fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }
}
