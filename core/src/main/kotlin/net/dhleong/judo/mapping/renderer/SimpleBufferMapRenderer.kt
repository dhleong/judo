package net.dhleong.judo.mapping.renderer

import net.dhleong.judo.mapping.DEFAULT_MIN_MAP_HEIGHT
import net.dhleong.judo.mapping.DEFAULT_MIN_MAP_WIDTH
import net.dhleong.judo.mapping.IJudoMap
import net.dhleong.judo.mapping.IJudoRoom
import net.dhleong.judo.mapping.MapGrid
import net.dhleong.judo.render.Flavor
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoAppendable
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.SimpleFlavor

/**
 * @author dhleong
 */
class SimpleBufferMapRenderer(
    mapGrid: MapGrid = MapGrid(DEFAULT_MIN_MAP_WIDTH, DEFAULT_MIN_MAP_HEIGHT)
) : BufferMapRenderer(mapGrid) {

    override val name: String = "simple"
    override val charsPerX = 5
    override val charsPerY = 3

    internal val roadColor = SimpleFlavor(foreground = JudoColor.Simple(JudoColor.Simple.Color.WHITE))
    internal val stairColor = SimpleFlavor(
        isBold = true,
        foreground = JudoColor.Simple(JudoColor.Simple.Color.WHITE)
    )
    internal val wallColor = SimpleFlavor(
        isBold = true,
        foreground = JudoColor.Simple(JudoColor.Simple.Color.WHITE)
    )
    internal val hereColor = SimpleFlavor(
        isBold = true,
        foreground = JudoColor.Simple(JudoColor.Simple.Color.RED)
    )
    internal val noColor = Flavor.default

    private val buffer = FlavorableStringBuilder(512)
    private var currentRoomId: Int = -1
    private lateinit var currentGrid: MapGrid

    override fun appendGridInto(map: IJudoMap, grid: MapGrid, window: IJudoAppendable) {
        currentGrid = grid
        currentRoomId = map.inRoom ?: map.lastRoom

        for (y in 0..(grid.height - 1)) {
            window.appendLine(buildLineTop(y))
            window.appendLine(buildLineCenter(y))
            window.appendLine(buildLineBottom(y))
        }
    }

    private fun buildLineTop(y: Int) = buildRow(y) { room ->
        val nw = "nw" in room.exits
        val n = "n" in room.exits
        val u = "u" in room.exits
        val ne = "ne" in room.exits

        if (nw) append("\\ ", roadColor)
        else append("  ", noColor)

        if (n) append("|", roadColor)
        else append(" ", noColor)

        if (u) append("+", stairColor)
        else append(" ", noColor)

        if (ne) append("/", roadColor)
        else if (nw || n || u) append(" ", noColor)
        else append(" ", noColor)
    }

    private fun buildLineCenter(y: Int) = buildRow(y) { room ->
        val w = "w" in room.exits
        val e = "e" in room.exits
        val here = room.id == currentRoomId

        if (w) {
            append("-", roadColor)
            append("[", wallColor)
        }
        else append(" [", wallColor)

        if (here) append("#", hereColor)
        else append(" ", noColor)

        when {
            e -> {
                append("]", wallColor)
                append("-", roadColor)
            }
            here -> append("] ", wallColor)
            else -> append("] ", noColor)
        }
    }

    private fun buildLineBottom(y: Int) = buildRow(y) { room ->
        val sw = "sw" in room.exits
        val s = "s" in room.exits
        val d = "d" in room.exits
        val se = "se" in room.exits

        if (sw) append("/", roadColor)
        else append(" ", noColor)

        if (d) append("-", stairColor)
        else append(" ", noColor)

        if (s) append("|", roadColor)
        else append(" ", noColor)

        if (se) append(" \\", roadColor)
        else if (sw || s || d) append("  ", noColor)
        else append("  ", noColor)
    }

    private inline fun buildRow(y: Int, block: FlavorableStringBuilder.(room: IJudoRoom) -> Unit): FlavorableCharSequence =
        withBuffer {
            val grid = currentGrid
            grid.forEachCol { col ->
                val room = grid[col, y]
                if (room == null) {
                    append("     ", noColor)
                } else {
                    block(this, room)
                }
            }
        }

    private inline fun MapGrid.forEachCol(block: (col: Int) -> Unit) {
        for (x in 0..(width - 1)) {
            block(x)
        }
    }

    private inline fun withBuffer(block: FlavorableStringBuilder.() -> Unit): FlavorableCharSequence {
        buffer.reset()
        block(buffer)
        return buffer.toFlavorableString()
    }

}
