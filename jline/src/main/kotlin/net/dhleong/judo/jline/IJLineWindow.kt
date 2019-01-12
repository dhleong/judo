package net.dhleong.judo.jline

import net.dhleong.judo.render.IJudoWindow

/**
 * @author dhleong
 */
interface IJLineWindow : IJudoWindow {
    fun render(display: JLineDisplay, x: Int, y: Int)
}
