package net.dhleong.judo.mapping

import assertk.assertThat
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import org.junit.Test

/**
 * @author dhleong
 */
class MapGridTest {
    @Test fun simple() {
        val map = JudoMap()
        val center = JudoRoom(0, "0")
        val left = JudoRoom(1, "1")
        val right = JudoRoom(2, "2")
        val veryLeft = JudoRoom(3, "3")
        val top = JudoRoom(4, "4")
        val up = JudoRoom(5, "5")

        map.add(center)
        map.dig(center, "w", left)
        map.dig(center, "e", right)
        map.dig(center, "n", top)
        map.dig(center, "u", up)
        map.dig(left, "w", veryLeft)

        val grid = MapGrid(6, 4)
        grid.buildAround(map, center)

        assertThat(grid[0, 0]).isNull()
        assertThat(grid[0, 1]).isNull()
        assertThat(grid[0, 2]).isNull()
        assertThat(grid[0, 3]).isNull()

        assertThat(grid[1, 0]).isNull()
        assertThat(grid[1, 1]).isNull()
        assertThat(grid[1, 2]).isSameAs(veryLeft)
        assertThat(grid[1, 3]).isNull()

        assertThat(grid[2, 0]).isNull()
        assertThat(grid[2, 1]).isNull()
        assertThat(grid[2, 2]).isSameAs(left)
        assertThat(grid[2, 3]).isNull()

        assertThat(grid[3, 0]).isNull()
        assertThat(grid[3, 1]).isSameAs(top)
        assertThat(grid[3, 2]).isSameAs(center)
        assertThat(grid[3, 3]).isNull()

        assertThat(grid[4, 0]).isNull()
        assertThat(grid[4, 1]).isNull()
        assertThat(grid[4, 2]).isSameAs(right)
        assertThat(grid[4, 3]).isNull()

        assertThat(grid[5, 0]).isNull()
        assertThat(grid[5, 1]).isNull()
        assertThat(grid[5, 2]).isNull()
        assertThat(grid[5, 3]).isNull()
    }

    @Test fun tunnel() {
        val map = JudoMap()
        val center = JudoRoom(0, "0")
        val down = JudoRoom(1, "1")
        val dl = JudoRoom(2, "2")
        val dvl = JudoRoom(3, "3")
        val up = JudoRoom(4, "4")

        map.add(center)
        map.dig(center, "d", down)
        map.dig(down, "w", dl)
        map.dig(dl, "w", dvl)
        map.dig(dvl, "u", up)

        val grid = MapGrid(4, 4)
        grid.buildAround(map, center)

        assertThat(grid[0, 0]).isNull()
        assertThat(grid[0, 1]).isNull()
        assertThat(grid[0, 2]).isSameAs(up)
        assertThat(grid[0, 3]).isNull()

        assertThat(grid[1, 0]).isNull()
        assertThat(grid[1, 1]).isNull()
        assertThat(grid[1, 2]).isNull()
        assertThat(grid[1, 3]).isNull()

        assertThat(grid[2, 0]).isNull()
        assertThat(grid[2, 1]).isNull()
        assertThat(grid[2, 2]).isSameAs(center)
        assertThat(grid[2, 3]).isNull()

        assertThat(grid[3, 0]).isNull()
        assertThat(grid[3, 1]).isNull()
        assertThat(grid[3, 2]).isNull()
        assertThat(grid[3, 3]).isNull()
    }
}