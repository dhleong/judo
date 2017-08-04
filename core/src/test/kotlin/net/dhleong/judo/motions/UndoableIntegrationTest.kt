package net.dhleong.judo.motions

import net.dhleong.judo.input.keys
import net.dhleong.judo.setInput
import net.dhleong.judo.type
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

/**
 * @author dhleong
 */
class UndoableIntegrationTest : AbstractMotionIntegrationTest() {

    @Ignore("FIXME")
    @Test fun repeatChange() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("cfdfun<esc>"))
        assertThat(renderer.inputLine)
            .isEqualTo("fun word2 word3" to 2)

        judo.type(keys("w.w."))
        assertThat(renderer.outputLines).isEmpty() // no errors
        assertThat(renderer.inputLine)
            .isEqualTo("fun fun2 fun3" to 12)
    }

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

    @Test fun repeatInsert() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("inew<esc>"))
        assertThat(renderer.inputLine)
            .isEqualTo("newword word2 word3" to 2)

        judo.type(keys("w."))
        assertThat(renderer.inputLine)
             .isEqualTo("newword newword2 word3" to 10)

        judo.type(keys("w."))
        assertThat(renderer.outputLines).isEmpty() // no errors
        assertThat(renderer.inputLine)
            .isEqualTo("newword newword2 newword3" to 19)
    }
}