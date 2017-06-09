package net.dhleong.judo.complete

import net.dhleong.judo.input.InputBuffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
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

    @Test fun updateWithSuggestions() {
        val buffer = InputBuffer()
        buffer.set("  l")
        buffer.cursor = 3
        suggester.initialize("  l", 3)

        suggester.updateWithNextSuggestion(buffer)
        assertThat(buffer.toString()).isEqualTo("  land")

        suggester.updateWithNextSuggestion(buffer)
        assertThat(buffer.toString()).isEqualTo("  love")

        suggester.updateWithPrevSuggestion(buffer)
        assertThat(buffer.toString()).isEqualTo("  land")

        suggester.updateWithPrevSuggestion(buffer)
        assertThat(buffer.toString()).isEqualTo("  l")
    }
}