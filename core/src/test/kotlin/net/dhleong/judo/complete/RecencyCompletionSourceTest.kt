package net.dhleong.judo.complete

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class RecencyCompletionSourceTest {

    lateinit var source: CompletionSource

    @Before fun setUp() {
        // monotonically increasing time for testability
        var time: Long = 0
        source = RecencyCompletionSource(maxCandidates = 8) {
            time++
        }
        source.process("She sells seashells by the sea shore")
        source.process("She does!")
    }

    @Test fun noMatching() {
        assertThat(source.suggest("stand").toList())
            .isEmpty()
    }

    @Test fun noDups() {
        assertThat(source.suggest("she").toList())
            .containsExactly("she")
    }

    @Test fun multiple() {
        // sorted by recency, not alphabet
        assertThat(source.suggest("s").toList())
            .containsExactly(
                "she",
                "shore",
                "sea",
                "seashells",
                "sells"
            )
    }

    @Test fun trimOlder() {
        // NOTE: breaking the black box a bit here, but since
        // we dedup lazily rather than eagerly during insert,
        // adding `super` actually prunes off the old `she`;
        // then, `special` prunes off `sells`.

        source.process("super special")
        assertThat(source.suggest("s").toList())
            .containsExactly(
                "special",
                "super",
                "she",
                "shore",
                "sea",
                "seashells"
            )
    }
}