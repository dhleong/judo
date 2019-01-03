package net.dhleong.judo.jline

import net.dhleong.judo.render.IJudoWindow2

/**
 * @author dhleong
 */
interface IJLineWindow : IJudoWindow2 {
    fun render(display: JLineDisplay, x: Int, y: Int)
}
