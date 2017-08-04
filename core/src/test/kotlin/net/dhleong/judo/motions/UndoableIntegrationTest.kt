package net.dhleong.judo.motions

import net.dhleong.judo.input.keys
import net.dhleong.judo.setInput
import net.dhleong.judo.type
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * @author dhleong
 */
class UndoableIntegrationTest : AbstractMotionIntegrationTest() {

    @Test fun repeatDeleteWord() {
        judo.setInput("word word2 word3 word4", 0)

        judo.type(keys("dw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word2 word3 word4" to 0)

        judo.type(keys(".."))
        assertThat(renderer.outputLines).isEmpty() // no errors
        assertThat(renderer.inputLine)
            .isEqualTo("word4" to 0)
    }
}