package net.dhleong.judo

import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.render.JudoTabpage
import net.dhleong.judo.render.PrimaryJudoWindow
import net.dhleong.judo.render.getAnsiContents
import java.lang.reflect.Proxy

/**
 * @author dhleong
 */

class TestableJudoRenderer(
    private val state: RendererState = RendererState()
) : JudoRenderer by createRendererProxy(state) {

    var settableWindowWidth: Int
        get() = state.windowWidth
        set(value) { state.windowWidth = value }

    val inputLine: Pair<String, Int>
        get() = state.inputLine

    val mapRenderer: MapRenderer = Proxy { _, _ -> }

    val output: JudoBuffer
        get() = state.output
    val outputLines: List<String>
        get() = state.output.getAnsiContents()
}

class RendererState(var windowWidth: Int = 90, var windowHeight: Int = 30) {

    val settings = StateMap()
    val ids = IdManager()
    val output = JudoBuffer(ids)
    val primaryWindow = PrimaryJudoWindow(ids, settings, output, windowWidth, windowHeight)
    val tabpage = JudoTabpage(ids, settings, primaryWindow)
    var inputLine: Pair<String, Int> = "" to 0
}

private fun createRendererProxy(state: RendererState): JudoRenderer {
    return Proxy { method, args ->
        when (method.name) {

            "getWindowWidth" -> state.windowWidth
            "getWindowHeight" -> state.windowHeight

            "getCurrentTabpage" -> state.tabpage

            "inTransaction" -> {
                @Suppress("UNCHECKED_CAST")
                val block = args[0] as () -> Unit
                block()
            }

            "updateInputLine" -> {
                state.inputLine = args[0] as String to args[1] as Int
            }

            else -> null // ignore
        }
    }
}
