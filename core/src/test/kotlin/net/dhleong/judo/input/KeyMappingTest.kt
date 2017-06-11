package net.dhleong.judo.input

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * @author dhleong
 */
class KeyMappingTest {
    @Test fun sequenceMap() {
        val map = KeyMapping(
            keys("cd") to { _ -> }
        )

        assertThat(map.match(keys("c"))).isNull()
        assertThat(map.couldMatch(keys("c"))).isTrue()
    }

    @Test fun sequenceUnmap() {
        val map = KeyMapping(
            keys("cd") to { _ -> },
            keys("ce") to { _ -> }
        )

        assertThat(map.match(keys("c"))).isNull()
        assertThat(map.couldMatch(keys("c"))).isTrue()

        map.unmap(keys("cd"))

        // we still might match c!
        assertThat(map.match(keys("c"))).isNull()
        assertThat(map.couldMatch(keys("c"))).isTrue()

        // definitely still match ce
        assertThat(map.match(keys("ce"))).isNotNull()
        assertThat(map.couldMatch(keys("ce"))).isTrue()

        // but not cd
        assertThat(map.match(keys("cd"))).isNull()
        assertThat(map.couldMatch(keys("cd"))).isFalse()

        // now, nothing
        map.unmap(keys("ce"))

        // we still might match c!
        assertThat(map.match(keys("c"))).isNull()
        assertThat(map.couldMatch(keys("c"))).isFalse()
        assertThat(map.couldMatch(keys("ce"))).isFalse()

    }
}