package net.dhleong.judo.motions

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.dhleong.judo.input.keys
import net.dhleong.judo.setInput
import net.dhleong.judo.type
import org.junit.Test

/**
 * @author dhleong
 */
class PairwiseMotionIntegrationTest : AbstractMotionIntegrationTest() {

    @Test fun deleteInnerQuotes() {
        judo.setInput("word \"word2\" word3", 8)

        judo.type(keys("di\""))
        assertThat(renderer.inputLine)
            .isEqualTo("word \"\" word3" to 6)
    }

    @Test fun deleteInnerQuotes_hanging() {
        judo.setInput("word \"", 0)

        judo.type(keys("di\""))
        assertThat(renderer.inputLine)
            .isEqualTo("word \"" to 0)
    }

    @Test fun deleteInnerQuotes_nothing() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("di\""))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 0)
    }

    @Test fun deleteInnerQuotes_searchForPair() {
        judo.setInput("word \"word2\" word3", 0)

        judo.type(keys("di\""))
        assertThat(renderer.inputLine)
            .isEqualTo("word \"\" word3" to 6)
    }

    @Test fun deleteInnerParens() {
        judo.setInput("word (word2) word3", 5)

        judo.type(keys("dib"))
        assertThat(renderer.inputLine)
            .isEqualTo("word () word3" to 6)
    }

    @Test fun deleteInnerParens_searchForPair() {
        judo.setInput("word (word2) word3", 0)

        judo.type(keys("dib"))
        assertThat(renderer.inputLine)
            .isEqualTo("word () word3" to 6)
    }


    @Test fun deleteOuterQuotes() {
        judo.setInput("word \"word2\" word3", 8)

        judo.type(keys("da\""))
        assertThat(renderer.inputLine)
            .isEqualTo("word  word3" to 5)
    }

    @Test fun deleteOuterQuotes_onSecondPiece() {
        judo.setInput("word \"word2\" word3", 11)

        judo.type(keys("da\""))
        assertThat(renderer.inputLine)
            .isEqualTo("word  word3" to 5)
    }

    @Test fun deleteOuterQuotes_all() {
        judo.setInput("\"word2\"", 0)

        judo.type(keys("da\""))
        assertThat(renderer.inputLine)
            .isEqualTo("" to 0)
    }

    @Test fun deleteOuterQuotes_hanging() {
        judo.setInput("word \"", 0)

        judo.type(keys("da\""))
        assertThat(renderer.inputLine)
            .isEqualTo("word \"" to 0)
    }

    @Test fun deleteOuterQuotes_nothing() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("da\""))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 0)
    }

    @Test fun deleteOuterQuotes_searchForPair() {
        judo.setInput("word \"word2\" word3", 0)

        judo.type(keys("da\""))
        assertThat(renderer.inputLine)
            .isEqualTo("word  word3" to 5)
    }
}