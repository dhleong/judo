package net.dhleong.judo.jline

import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoWindow

/**
 * @author dhleong
 */
interface IJLineWindow : IJudoWindow {
    /**
     * Single-line echo
     */
    fun echo(text: FlavorableCharSequence)
    fun clearEcho()

    fun render(display: JLineDisplay, x: Int, y: Int)
}
