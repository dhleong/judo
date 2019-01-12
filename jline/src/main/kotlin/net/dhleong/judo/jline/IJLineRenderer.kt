package net.dhleong.judo.jline

import net.dhleong.judo.JudoRenderer2

/**
 * Additional capabilities for a JLine-based renderer
 * @author dhleong
 */
interface IJLineRenderer : JudoRenderer2 {
    fun onWindowResized(window: JLineWindow)
}