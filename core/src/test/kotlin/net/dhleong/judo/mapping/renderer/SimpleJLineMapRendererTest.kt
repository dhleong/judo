package net.dhleong.judo.mapping.renderer

import assertk.assert
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.hasLinesSomewhere
import net.dhleong.judo.mapping.JudoMap
import net.dhleong.judo.mapping.JudoRoom
import net.dhleong.judo.mapping.MapGrid
import net.dhleong.judo.render.Flavor
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class SimpleBufferMapRendererTest {
    val ids = IdManager()
    val buffer = JudoBuffer(ids)

    lateinit var renderer: SimpleBufferMapRenderer
    lateinit var roadColor: Flavor
    lateinit var stairColor: Flavor
    lateinit var wallColor: Flavor
    lateinit var hereColor: Flavor
    lateinit var noColor: Flavor

    @Before fun setUp() {
        renderer = SimpleBufferMapRenderer(
            TestableJudoRenderer()
        )
        buffer.clear()

        roadColor = renderer.roadColor
        stairColor = renderer.stairColor
        wallColor = renderer.wallColor
        hereColor = renderer.hereColor
        noColor = renderer.noColor
    }

    @Test fun renderSimple() {
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

        // render and verify
        renderer.appendGridInto(map, grid, buffer)
        assert(buffer).hasLinesSomewhere(
            "                [ ]           \n",
            "                 |            \n",
            "                 |+           \n",
            "      [#]--[ ]--[ ]--[ ]      \n"
        )

        // after verifying the shape, verify the colors
        // TODO
//        val ansi = buffer.getRawContents()
//        assertThat(ansi[4]).isEqualTo("                $wallColor[ ] $noColor          ")
//        assertThat(ansi[6]).isEqualTo("                 $roadColor|$stairColor+ $noColor          ")
//        assertThat(ansi)
//            .containsSequence(
//                "                $wallColor[ ] $noColor          ",
//                "                 $roadColor|  $noColor          ",
//                "                 $roadColor|$stairColor+ $noColor          ",
//                "      $wallColor[$hereColor#$wallColor]$roadColor-$noColor$roadColor-$wallColor[ ]$roadColor-$noColor$roadColor-$wallColor[ ]$roadColor-$noColor$roadColor-$wallColor[ ] $noColor     "
//            )
    }

    @Test fun renderTunnel() {
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

        // render and verify
        renderer.appendGridInto(map, grid, buffer)
        assert(buffer).hasLinesSomewhere(
            " [#]       [ ]      \n",
            " -         -        \n"
        )

    }
}