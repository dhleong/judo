package net.dhleong.judo.mapping

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import org.junit.Test

/**
 * @author dhleong
 */
class JudoMapTest {
    @Test fun deleteRoomCleansUpExits() {
        val map = JudoMap()
        val center = JudoRoom(0, "0")
        val left = JudoRoom(1, "1")
        val right = JudoRoom(2, "2")

        map.add(center)
        map.dig(center, "w", left)
        map.dig(center, "e", right)
        assertThat(left.exits.toList()).hasSize(1)
        assertThat(right.exits.toList()).hasSize(1)

        // delete and verify
        map.deleteRoom(center.id)
        assertThat(left.exits.asIterable()).isEmpty()
        assertThat(right.exits.asIterable()).isEmpty()
    }
}