package net.dhleong.judo.mapping

import java.util.ArrayDeque

private const val FLAG_VISITED = 32

const val DEFAULT_MIN_MAP_HEIGHT = 9
const val DEFAULT_MIN_MAP_WIDTH = 6

private const val MAX_Z_DISTANCE = 2

/**
 * @author dhleong
 */
class MapGrid(
    val width: Int,
    val height: Int
) {

    private val grid = arrayOfNulls<IJudoRoom>(width * height)
    private val buildQueue = ArrayDeque<BuildNode>(maxOf(width, height))

    operator fun get(x: Int, y: Int): IJudoRoom? {
        if (x < 0 || x >= width) throw IndexOutOfBoundsException("x: $x (width=$width)")
        if (y < 0 || y >= height) throw IndexOutOfBoundsException("y: $y (height=$height)")
        return grid[x + y * width]
    }

    fun buildAround(map: IJudoMap, centerRoom: IJudoRoom) {
        buildQueue.clear()
        buildQueue.add(BuildNode(
            centerRoom,
            x = width / 2,
            y = height / 2,
            z = 0,
            distance = 0
        ))
        grid.fill(null)

        // clear all the flags
        map.forEach { it.flags = it.flags and FLAG_VISITED.inv() }

        while (buildQueue.isNotEmpty()) {
            val node = buildQueue.pop()

            if (node.x in 0 until width && node.y in 0 until height && node.z == 0) {
                if (this[node] != null) continue

                this[node] = node.room
            }

            for (exit in node.room.exits) {
                if ((exit.room.flags and FLAG_VISITED) != 0) {
                    // we've already visited this room
                    continue
                }
                exit.room.flags += FLAG_VISITED

                val x = node.x + when {
                    (exit.dirs and IJudoExit.DIR_W) != 0 -> -1
                    (exit.dirs and IJudoExit.DIR_E) != 0 -> 1
                    else -> 0
                }
                val y = node.y + when {
                    (exit.dirs and IJudoExit.DIR_N) != 0 -> -1
                    (exit.dirs and IJudoExit.DIR_S) != 0 -> 1
                    else -> 0
                }
                val z = node.z + when {
                    (exit.dirs and IJudoExit.DIR_U) != 0 -> -1
                    (exit.dirs and IJudoExit.DIR_D) != 0 -> 1
                    else -> 0
                }

                if (x == node.x && y == node.y && z == node.z) {
                    continue
                }

                if (x < 0 || x >= width || y < 0 || y >= height) continue
                if (z < -MAX_Z_DISTANCE || z > MAX_Z_DISTANCE) continue

                buildQueue.add(BuildNode(exit.room, x, y, z, node.distance + 1))
            }
        }
    }


    private operator fun get(node: BuildNode): IJudoRoom? = this[node.x, node.y]
    internal operator fun set(x: Int, y: Int, room: IJudoRoom) {
        grid[x + y * width] = room
    }
    private operator fun set(node: BuildNode, room: IJudoRoom) {
        this[node.x, node.y] = room
    }

    private class BuildNode(
        val room: IJudoRoom,
        val x: Int,
        val y: Int,
        val z: Int,
        val distance: Int
    )
}

