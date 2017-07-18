package net.dhleong.judo

import net.dhleong.judo.mapping.IJudoMap
import net.dhleong.judo.mapping.IJudoRoom
import net.dhleong.judo.mapping.MapGrid
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.util.ansi

/**
 *
 * @author dhleong
 */
interface JLineMapRenderer {

    val name: String

    /**
     * Append a text representation of the map grid
     * to the window
     */
    fun appendGridInto(map: IJudoMap, grid: MapGrid, window: IJudoWindow)
}

class SimpleJLineMapRenderer : JLineMapRenderer {

    override val name: String = "simple"

    internal val roadColor = ansi(0, fg = 7)
    internal val stairColor = ansi(1, fg = 7)
    internal val wallColor = ansi(1, fg = 7)
    internal val hereColor = ansi(1, fg = 1)
    internal val noColor = ansi(0)

    private val buffer = StringBuilder(512)
    private var currentRoomId: Int = -1
    private lateinit var currentGrid: MapGrid

    override fun appendGridInto(map: IJudoMap, grid: MapGrid, window: IJudoWindow) {
        currentGrid = grid
        currentRoomId = map.inRoom ?: map.lastRoom

        for (y in 0..(grid.height - 1)) {
            window.appendLine(buildLineTop(y), isPartialLine = false)
            window.appendLine(buildLineCenter(y), isPartialLine = false)
            window.appendLine(buildLineBottom(y), isPartialLine = false)
        }
    }

    private fun buildLineTop(y: Int) = buildRow(y) { room ->
        val nw = "nw" in room.exits
        val n = "n" in room.exits
        val u = "u" in room.exits
        val ne = "ne" in room.exits

        if (nw) append("$roadColor\\ ")
        else append("  ")

        if (n) append("$roadColor|")
        else append(" ")

        if (u) append("$stairColor+")
        else append(" ")

        if (ne) append("$roadColor/$noColor")
        else if (nw || n || u) append(" $noColor")
        else append(" ")
    }

    private fun buildLineCenter(y: Int) = buildRow(y) { room ->
        val w = "w" in room.exits
        val e = "e" in room.exits
        val here = room.id == currentRoomId

        if (w) append("$roadColor-$wallColor[")
        else append(" $wallColor[")

        if (here) append("$hereColor#")
        else append(" ")

        if (e && here) append("$wallColor]$roadColor-$noColor")
        else if (e) append("]$roadColor-$noColor")
        else if (here) append("$wallColor] $noColor")
        else append("] $noColor")
    }

    private fun buildLineBottom(y: Int) = buildRow(y) { room ->
        val sw = "sw" in room.exits
        val s = "s" in room.exits
        val d = "d" in room.exits
        val se = "se" in room.exits

        if (sw) append("$roadColor/")
        else append(" ")

        if (d) append("$stairColor-")
        else append(" ")

        if (s) append("$roadColor|")
        else append(" ")

        if (se) append(" $roadColor\\$noColor")
        else if (sw || s || d) append("  $noColor")
        else append("  ")
    }

    private inline fun buildRow(y: Int, block: StringBuilder.(room: IJudoRoom) -> Unit): CharSequence =
        withBuffer {
            val grid = currentGrid
            grid.forEachCol { col ->
                val room = grid[col, y]
                if (room == null) {
                    append("     ")
                } else {
                    block(this, room)
                }
            }
        }

    private inline fun MapGrid.forEachCol(block: (col: Int) -> Unit): Unit {
        for (x in 0..(width - 1)) {
            block(x)
        }
    }

    private inline fun withBuffer(block: StringBuilder.() -> Unit): CharSequence {
        buffer.setLength(0)
        block(buffer)
        return buffer
    }

}
