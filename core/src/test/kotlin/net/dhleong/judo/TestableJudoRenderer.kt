package net.dhleong.judo

import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.render.IJudoBuffer2
import net.dhleong.judo.render.IJudoTabpage2
import net.dhleong.judo.render.IJudoWindow2
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer2
import net.dhleong.judo.render.PrimaryJudoWindow2

/**
 * @author dhleong
 */

class TestableJudoRenderer(
    private val state: RendererState = RendererState()
) : JudoRenderer2 by createRendererProxy(state) {

    var settableWindowWidth: Int
        get() = state.windowWidth
        set(value) { state.windowWidth = value }

    val inputLine: Pair<String, Int>
        get() = state.inputLine

    val mapRenderer: MapRenderer = Proxy { _, _ -> }

    val output: JudoBuffer2
        get() = state.output
    val outputLines: List<String>
        get() = (0..state.output.lastIndex).map { i ->
            // TODO ansi?
            state.output[i].toString().removeSuffix("\n")
        }
}

class RendererState(var windowWidth: Int = 90, var windowHeight: Int = 30) {

    val settings = StateMap()
    val ids = IdManager()
    val output = JudoBuffer2(ids)
    val primaryWindow = object : PrimaryJudoWindow2(ids, settings, output, windowWidth, windowHeight) {
        override fun createBuffer(ids: IdManager): IJudoBuffer2 = JudoBuffer2(ids)

        override fun createWindow(
            ids: IdManager,
            settings: StateMap,
            initialWidth: Int,
            initialHeight: Int,
            initialBuffer: IJudoBuffer2,
            isFocusable: Boolean,
            statusLineOverlaysOutput: Boolean
        ): IJudoWindow2 = object : IJudoWindow2 by dumbProxy() {
            override var currentBuffer: IJudoBuffer2
                get() = initialBuffer
                set(value) { TODO() }

            override var isFocused: Boolean = true
            override val isFocusable: Boolean = isFocusable
        }

    }
    val tabpage = object : IJudoTabpage2 by dumbProxy() {
        override var currentWindow: IJudoWindow2
            get() = primaryWindow
            set(value) { TODO() }
    }
    var inputLine: Pair<String, Int> = "" to 0
}

private inline fun <reified T> dumbProxy() = Proxy<T> { method, args -> null }

private fun createRendererProxy(state: RendererState): JudoRenderer2 {
    return Proxy { method, args ->
        when (method.name) {

            "getWindowWidth" -> state.windowWidth
            "getWindowHeight" -> state.windowHeight

            "getCurrentTabpage" -> state.tabpage

            "updateInputLine" -> {
                state.inputLine = args[0] as String to args[1] as Int
            }

            else -> null // ignore
        }
    }
}
