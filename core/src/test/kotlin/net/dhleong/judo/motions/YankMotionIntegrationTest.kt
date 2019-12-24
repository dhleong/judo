package net.dhleong.judo.motions

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import net.dhleong.judo.input.keys
import net.dhleong.judo.setInput
import net.dhleong.judo.type
import org.junit.Test

/**
 * @author dhleong
 */
class YankMotionIntegrationTest : AbstractMotionIntegrationTest() {
    @Test fun yankWord() {
        judo.setInput("word word2 word3", 5)

        judo.type(keys("yw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 5)
        assertThat(renderer.outputLines).isEmpty() // no error deleting last text
        assertThat(judo.registers.unnamed.value.toString())
            .isEqualTo("word2 ")
    }

    @Test fun YANK() {
        judo.setInput("word word2 word3", 5)

        judo.type(keys("Y"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 5)
        assertThat(renderer.outputLines).isEmpty() // no error deleting last text
        assertThat(judo.registers.unnamed.value.toString())
            .isEqualTo("word word2 word3")
    }

    @Test fun yankWordIntoRegister() {
        judo.setInput("word word2 word3", 5)

        judo.type(keys("\"ayw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 5)
        assertThat(renderer.outputLines).isEmpty() // no error deleting last text
        assertThat(judo.registers.unnamed.value).isEmpty()
        assertThat(judo.registers['a'].value.toString())
            .isEqualTo("word2 ")
    }

    @Test fun YANKIntoRegister() {
        judo.setInput("word word2 word3", 5)

        judo.type(keys("\"aY"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 5)
        assertThat(renderer.outputLines).isEmpty() // no error deleting last text
        assertThat(judo.registers.unnamed.value).isEmpty()
        assertThat(judo.registers['a'].value.toString())
            .isEqualTo("word word2 word3")
    }
}