package net.dhleong.judo.mapping

import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class DirectionalMapStrategyTest {

    lateinit var judo: TestableJudoCore
    lateinit var mapper: AutomagicMapper
    lateinit var strategy: MapStrategy
    lateinit var map: JudoMap

    @Before fun setUp() {
        judo = TestableJudoCore()
        mapper = AutomagicMapper(judo, judo.mapper)
        strategy = DirectionalMapStrategy(mapper)

        judo.mapper.createEmpty()
        map = (judo.mapper.current as JudoMap?)!!
        map.inRoom = 0
        map.add(JudoRoom(0, "0"))
    }

    @Test fun simpleFollow() {
        val firstRoom = map[0]!!
        assertThat(firstRoom.exits.size).isEqualTo(0)

        strategy.onMove("e")
        strategy.onRoom(1, "1", mapOf("west" to "O"))
        val newRoom = map.currentRoom!!

        assertThat(map.inRoom).isEqualTo(1)

        assertThat(firstRoom.exits.size).isEqualTo(1)
        assertThat(firstRoom.exits.contains("e")).isTrue()

        assertThat(newRoom.exits.size).isEqualTo(1)
        assertThat(newRoom.exits.contains("w")).isTrue()
    }

    @Test fun skipRoom() {
        val firstRoom = map[0]!!
        assertThat(firstRoom.exits.size).isEqualTo(0)

        strategy.onMove("e")
        strategy.onMove("e")
        strategy.onRoom(2, "2", mapOf("west" to "O"))
        val newRoom = map.currentRoom!!

        assertThat(map.inRoom).isEqualTo(2)

        assertThat(firstRoom.exits.size).isEqualTo(0)
        assertThat(newRoom.exits.size).isEqualTo(0)
    }

    @Test fun skipPastKnownRoom() {
        val firstRoom = map[0]!!
        val middleRoom = JudoRoom(1, "1")
        map.dig(firstRoom, "e", middleRoom)
        assertThat(firstRoom.exits.size).isEqualTo(1)

        strategy.onMove("e")
        strategy.onMove("e")
        strategy.onRoom(2, "2", mapOf("west" to "O"))
        val newRoom = map.currentRoom!!

        assertThat(map.inRoom).isEqualTo(2)

        assertThat(firstRoom.exits.size).isEqualTo(1)
        assertThat(firstRoom.exits.contains("e")).isTrue()

        assertThat(newRoom.exits.size).isEqualTo(1)
        assertThat(newRoom.exits.contains("w")).isTrue()

        assertThat(middleRoom.exits.size).isEqualTo(2)
        assertThat(middleRoom.exits.contains("w")).isTrue()
        assertThat(middleRoom.exits.contains("e")).isTrue()
    }

}