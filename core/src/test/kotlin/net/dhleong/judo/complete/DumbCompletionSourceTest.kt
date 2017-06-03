package net.dhleong.judo.complete

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class DumbCompletionSourceTest {

    lateinit var source: CompletionSource

    @Before fun setUp() {
        source = DumbCompletionSource()
        source.process("Take my love, take my land")
    }

    @Test fun noMatching() {
        assertThat(source.suggest("stand").toList())
            .isEmpty()
    }

    @Test fun noDups() {
        assertThat(source.suggest("ta").toList())
            .containsExactly("take")
    }

    @Test fun multiple() {
        assertThat(source.suggest("l").toList())
            .containsExactly(
                "land",
                "love"
            )
    }
}