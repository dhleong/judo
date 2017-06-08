package net.dhleong.judo

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

    val outputLines: MutableList<Pair<String, Boolean>>
        get() = state.outputLines
}

class RendererState(var windowWidth: Int = 90) {
    val outputLines = mutableListOf<Pair<String, Boolean>>()
    var inputLine: Pair<String, Int> = "" to 0
}

private fun createRendererProxy(state: RendererState): JudoRenderer {
    return Proxy.newProxyInstance(
        ClassLoader.getSystemClassLoader(),
        arrayOf(JudoRenderer::class.java)
    ) { _, method, args ->
        when (method.name) {
            "appendOutput" -> {
                val line = args[0] as CharSequence
                val isPartial = args[1] as Boolean

                state.outputLines.add(line.toString() to isPartial)
                args[0] // return the charsequence
            }

            "getWindowWidth" -> state.windowWidth

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
    } as JudoRenderer
}
