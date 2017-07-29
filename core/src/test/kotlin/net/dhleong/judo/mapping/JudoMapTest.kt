package net.dhleong.judo.mapping

import org.assertj.core.api.Assertions.assertThat
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
        assertThat(left.exits.asIterable()).hasSize(1)
        assertThat(right.exits.asIterable()).hasSize(1)

        // delete and verify
        map.deleteRoom(center.id)
        assertThat(left.exits.asIterable()).isEmpty()
        assertThat(right.exits.asIterable()).isEmpty()
    }
}