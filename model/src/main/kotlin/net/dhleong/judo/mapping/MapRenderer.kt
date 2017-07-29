package net.dhleong.judo.mapping

import net.dhleong.judo.render.IJudoWindow

/**
 * @author dhleong
 */
interface MapRenderer {
    fun resize(width: Int = -1, height: Int = -1)
    fun renderMap(map: IJudoMap, window: IJudoWindow? = null)
}