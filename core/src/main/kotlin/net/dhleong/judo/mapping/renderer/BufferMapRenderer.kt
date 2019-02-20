package net.dhleong.judo.mapping.renderer

import net.dhleong.judo.mapping.DEFAULT_MIN_MAP_HEIGHT
import net.dhleong.judo.mapping.DEFAULT_MIN_MAP_WIDTH
import net.dhleong.judo.mapping.IJudoMap
import net.dhleong.judo.mapping.MapGrid
import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.render.IJudoAppendable
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.PrimaryJudoWindow

/**
 * Base class for [MapRenderer] implementations that
 * render a text-based map to an [IJudoAppendable]
 *
 * @author dhleong
 */
abstract class BufferMapRenderer(
    protected var mapGrid: MapGrid
) : MapRenderer {

    abstract val name: String
    abstract val charsPerX: Int
    abstract val charsPerY: Int

    /**
     * Append a text representation of the map grid to the [window]
     */
    abstract fun appendGridInto(map: IJudoMap, grid: MapGrid, window: IJudoAppendable)

    override fun renderMap(map: IJudoMap, window: IJudoWindow) {
        if (window !is PrimaryJudoWindow) {
            window.currentBuffer.clear()
        }

        map.currentRoom?.let { room ->
            mapGrid.buildAround(map, room)

            // if provided a window, use it; otherwise, try to use
            // the current window
            appendGridInto(
                map,
                mapGrid,
                window
            )
        }
    }

    override fun resize(width: Int, height: Int) {
        val newWidth = maxOf(
            DEFAULT_MIN_MAP_WIDTH,
            if (width == -1) mapGrid.width
            else width
        )
        val newHeight = maxOf(
            DEFAULT_MIN_MAP_HEIGHT,
            if (height == -1) mapGrid.height
            else height
        )

        if (newWidth == mapGrid.width && newHeight == mapGrid.height) {
            // nop
            return
        }

        mapGrid = MapGrid(newWidth / charsPerX, newHeight / charsPerY)
    }
}
