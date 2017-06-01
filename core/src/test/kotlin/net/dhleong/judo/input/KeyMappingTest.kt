package net.dhleong.judo.input

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * @author dhleong
 */
class KeyMappingTest {
    @Test fun sequenceMap() {
        val map = KeyMapping(
            keys("c", "d") to { _ -> }
        )

        assertThat(map.match(keys("c"))).isNull()
        assertThat(map.couldMatch(keys("c"))).isTrue()
    }
}