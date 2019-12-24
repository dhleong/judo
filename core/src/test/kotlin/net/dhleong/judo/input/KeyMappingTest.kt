package net.dhleong.judo.input

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.Test

/**
 * @author dhleong
 */
class KeyMappingTest {
    @Test fun sequenceMap() {
        val map = KeyMapping(
            keys("cd") to action { /* nop */ }
        )

        assertThat(map.match(keys("c"))).isNull()
        assertThat(map.couldMatch(keys("c"))).isTrue()
    }

    @Test fun sequenceUnmap() {
        val map = KeyMapping(
            keys("cd") to action { /* nop */ },
            keys("ce") to action { /* nop */ }
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