package net.dhleong.judo.complete

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class MarkovCompletionSourceTest {

    lateinit var source: MarkovCompletionSource

    @Before fun setUp() {
        source = MarkovCompletionSource(stopWords = DEFAULT_STOP_WORDS)

        // process in weird order to demonstrate it's frequency based,
        // not insert-order or alpha or anything
        source.process("Take my love")
        source.process("I'm still free")
        source.process("Take me where I cannot stand")
        source.process("I don't care")
        source.process("Take my land")
    }

    @Test fun emptyCompletions() {
        assertThat(source.suggest("").toList())
            .containsExactly("take", "i", "i'm")
    }

    @Test fun firstFilteredCompletions() {
        assertThat(source.suggest("t").toList())
            .containsExactly("take")
    }

    @Test fun sequenceCompletions() {
        assertThat(source.suggest("take m", 5..5).toList())
            .containsExactly("my", "me")

        assertThat(source.suggest("take my l", 8..8).toList())
            // no particular order here since use counts are the same:
            .containsExactlyInAnyOrder("land", "love")
    }

    @Test fun ignoreStopWords() {
        source.process("say Hello")
        assertThat(source.suggest("").toList())
            .containsExactly("take", "i", "i'm")
    }

}