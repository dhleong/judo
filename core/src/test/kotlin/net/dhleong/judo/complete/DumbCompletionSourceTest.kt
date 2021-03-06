package net.dhleong.judo.complete

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
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

    @Test fun nonNormalized() {
        source = DumbCompletionSource(normalize = false)
        source.process("logToFile")

        assertThat(source.suggest("l").toList())
            .containsExactly("logToFile")

        assertThat(source.suggest("logto").toList())
            .containsExactly("logToFile")

        assertThat(source.suggest("logtof").toList())
            .containsExactly("logToFile")
    }
}