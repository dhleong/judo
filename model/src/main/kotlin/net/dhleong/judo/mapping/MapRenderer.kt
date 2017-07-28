package net.dhleong.judo.mapping

/**
 * @author dhleong
 */
interface MapRenderer {
    fun resize(width: Int = -1, height: Int = -1)
    fun renderMap(map: IJudoMap)
}