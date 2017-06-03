package net.dhleong.judo.complete

import org.assertj.core.api.Assertions.assertThat
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