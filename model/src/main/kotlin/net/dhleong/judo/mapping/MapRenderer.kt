package net.dhleong.judo.mapping

import net.dhleong.judo.render.IJudoWindow2

/**
 * @author dhleong
 */
interface MapRenderer {
    fun resize(width: Int = -1, height: Int = -1)
    fun renderMap(map: IJudoMap, window: IJudoWindow2? = null)
}