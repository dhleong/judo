package net.dhleong.judo.complete

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class RecencyCompletionSourceTest {

    lateinit var source: CompletionSource

    @Before fun setUp() {
        source = RecencyCompletionSource(maxCandidates = 8)
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

    @Test fun processWhileSuggesting() {
        val iterator = source.suggest("s").iterator()
        assertThat(iterator.next()).isEqualTo("she")
        assertThat(iterator.next()).isEqualTo("shore")

        source.process("ignore")
        assertThat(iterator.next()).isEqualTo("sea")
        assertThat(iterator.next()).isEqualTo("seashells")
    }
}