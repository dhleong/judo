package net.dhleong.judo.complete

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * @author dhleong
 */
class CompletionSuggesterTest {

    val completions = DumbCompletionSource()
    val suggester = CompletionSuggester(completions)

    @Before fun setUp() {
        completions.process("Take my love, Take my land")
    }

    @Test fun nextSuggestion() {
        suggester.initialize("  l", 3)

        assertThat(suggester.nextSuggestion()).isEqualTo("land")
        assertThat(suggester.nextSuggestion()).isEqualTo("love")

        // loop the last available suggestion
        assertThat(suggester.nextSuggestion()).isEqualTo("love")
    }

    @Test fun prevSuggestion() {
        suggester.initialize("  l", 3)

        // prev should restore the original
        assertThat(suggester.nextSuggestion()).isEqualTo("land")
        assertThat(suggester.prevSuggestion()).isEqualTo("l")

        assertThat(suggester.nextSuggestion()).isEqualTo("land")
        assertThat(suggester.nextSuggestion()).isEqualTo("love")

        assertThat(suggester.prevSuggestion()).isEqualTo("land")
    }

    @Test fun noSuggestions() {
        suggester.initialize("  z", 3)

        // prev should restore the original
        assertThat(suggester.nextSuggestion()).isEqualTo("z")
        assertThat(suggester.prevSuggestion()).isEqualTo("z")
    }

    @Ignore("TODO")
    @Test fun updateWithSuggestions() {
        // TODO test this
    }
}