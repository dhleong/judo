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
class PasteMotionIntegrationTest : AbstractMotionIntegrationTest() {

    @Test fun deleteAndPasteForward() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("dwwp"))
        assertThat(judo.registers.unnamed.value.toString())
            .isEqualTo("word ")
        assertThat(renderer.inputLine)
            .isEqualTo("word2 wword ord3" to 11)
    }

    @Test fun deleteAndPasteBack() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("dwwP"))
        assertThat(judo.registers.unnamed.value.toString())
            .isEqualTo("word ")
        assertThat(renderer.inputLine)
            .isEqualTo("word2 word word3" to 11)
    }

    @Test fun deleteAndPasteEmpty() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("ddp"))
        assertThat(judo.registers.unnamed.value.toString())
            .isEqualTo("word word2 word3")
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 16)
    }

    @Test fun deleteAndPasteEmpty_back() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("ddP"))
        assertThat(judo.registers.unnamed.value.toString())
            .isEqualTo("word word2 word3")
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 16)
    }

    @Test fun deleteAndPasteFromRegister() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("\"adw"))
        assertThat(judo.registers.unnamed.value.toString())
            .isEqualTo("")
        assertThat(judo.registers['a'].value.toString())
            .isEqualTo("word ")

        // ensure the register selection doesn't bleed
        judo.type(keys("dw"))
        assertThat(judo.registers.unnamed.value.toString())
            .isEqualTo("word2 ")
        assertThat(judo.registers['a'].value.toString())
            .isEqualTo("word ")

        judo.type(keys("\"aP"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word3" to 5)

        // pasting should also reset the current register properly
        judo.type(keys("P"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 11)
    }
}