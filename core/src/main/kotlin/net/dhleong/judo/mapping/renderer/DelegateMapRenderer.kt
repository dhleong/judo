package net.dhleong.judo.mapping.renderer

import net.dhleong.judo.mapping.IJudoMap
import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.render.IJudoWindow

/**
 * Abstracts Map rendering across multiple potential
 * implementations based on the user's preferred style
 * @author dhleong
 */
class DelegateMapRenderer(
    // TODO multiple?
    private val delegate: MapRenderer
) : MapRenderer {

    override fun resize(width: Int, height: Int) {
        delegate.resize(width, height)
    }

    override fun renderMap(map: IJudoMap, window: IJudoWindow?) {
        delegate.renderMap(map, window)
    }

}