package net.dhleong.judo.complete

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import org.junit.Test

/**
 * @author dhleong
 */
class CompletionTokenizerKtTest {
    @Test fun emptyString() {
        assertThat(tokensFrom("").toList())
            .isEmpty()
    }

    @Test fun justSymbols() {
        assertThat(tokensFrom("( *$ ][").toList())
            .isEmpty()
    }

    @Test fun words() {
        assertThat(tokensFrom("You can't (take)").toList())
            .containsExactly(
                "You",
                "can't",
                "take"
            )
    }
}