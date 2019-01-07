package net.dhleong.judo.mapping.renderer

import net.dhleong.judo.JudoRenderer2
import net.dhleong.judo.inTransaction
import net.dhleong.judo.mapping.DEFAULT_MIN_MAP_HEIGHT
import net.dhleong.judo.mapping.DEFAULT_MIN_MAP_WIDTH
import net.dhleong.judo.mapping.IJudoMap
import net.dhleong.judo.mapping.MapGrid
import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.render.IJudoBuffer2
import net.dhleong.judo.render.IJudoWindow2

/**
 * Base class for [MapRenderer] implementations that
 * render a text-based map to a Buffer
 *
 * @author dhleong
 */
abstract class BufferMapRenderer(
    protected val renderer: JudoRenderer2,
    protected var mapGrid: MapGrid
) : MapRenderer {

    abstract val name: String
    abstract val charsPerX: Int
    abstract val charsPerY: Int

    /**
     * Append a text representation of the map grid
     * to the buffer
     */
    abstract fun appendGridInto(map: IJudoMap, grid: MapGrid, buffer: IJudoBuffer2)

    override fun renderMap(map: IJudoMap, window: IJudoWindow2?) {
        renderer.inTransaction {
            map.currentRoom?.let { room ->
                mapGrid.buildAround(map, room)

                // if provided a window, use it; otherwise, try to use
                // the current window
                appendGridInto(
                    map,
                    mapGrid,
                    (window ?: renderer.currentTabpage.currentWindow)
                        .currentBuffer
                )
            }
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
        mapGrid = MapGrid(newWidth / charsPerX, newHeight / charsPerY)
    }
}
