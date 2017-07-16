package net.dhleong.judo.mapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * @author dhleong
 */
class TintinMapFormatTest {
    @Test fun read() {
        val mapText = """
C 50000

CE <278>
CH <118>
CP <138>
CR <178>
CB

F 8

I 48

L * # #  # |   #        x


R {    1} {0} {} {} { } {} {} {} {} {} {1.000}
E {    2} {e} {e} {2} {0} {}
E {   37} {sw} {sw} {12} {0} {}
E {   39} {s} {s} {4} {0} {}

R {    2} {0} {} {} { } {} {} {} {} {} {1.000}
E {    1} {w} {w} {8} {0} {}
E {    3} {e} {e} {2} {0} {}
E {    4} {n} {n} {1} {0} {}
"""

        val map = JudoMap()
        TintinMapFormat().read(map, mapText.byteInputStream())

        assertThat(map.size).isEqualTo(6)
        assertThat(map[1]!!.exits["e"]!!.room)
            .isSameAs(map[2])

    }

    @Test fun writeAndRead() {
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

        val format = TintinMapFormat()
        val written = ByteArrayOutputStream(2048).let { out ->
            format.write(map, out)
            out.flush()
            out.toString()
        }

        val reconstituted = JudoMap()
        format.read(reconstituted, written.byteInputStream())

        assertThat(reconstituted.size).isEqualTo(map.size)
    }

    @Test fun splitArgsInBraces() {
        assertThat("cool {foo} {bar }  {biz}".splitArgsInBraces().toList())
            .containsExactly("foo", "bar ", "biz")
    }
}