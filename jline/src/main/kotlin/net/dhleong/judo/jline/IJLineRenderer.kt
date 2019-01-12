package net.dhleong.judo.jline

import net.dhleong.judo.JudoRenderer

/**
 * Additional capabilities for a JLine-based renderer
 * @author dhleong
 */
interface IJLineRenderer : JudoRenderer {
    fun onWindowResized(window: JLineWindow)
}