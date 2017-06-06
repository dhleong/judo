package net.dhleong.judo.motions

import net.dhleong.judo.JudoCore
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.input.keys
import net.dhleong.judo.setInput
import net.dhleong.judo.type
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class WordMotionIntegrationTest {

    val renderer = TestableJudoRenderer()
    lateinit var judo: JudoCore

    @Before fun setUp() {
        judo = JudoCore(renderer)
    }

    @Test fun moveFindBack() {
        judo.setInput("word word2 word3", 11)

        judo.type(keys("Fw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 5)
    }

    @Test fun moveFindForward() {
        judo.setInput("word word2 word3", 5)

        judo.type(keys("f<space>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 10)
    }

    @Test fun deleteFindBack() {
        judo.setInput("word word2 word3", 11)

        judo.type(keys("dFw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word3" to 5)
    }

    @Test fun deleteFindForward() {
        judo.setInput("word word2 word3", 5)

        judo.type(keys("df<space>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word3" to 5)
    }

    @Test fun deleteWord() {
        judo.setInput("word word2 word3", 16)

        judo.type(keys("<esc>bb"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 5)

        judo.type(keys("dw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word3" to 5)
    }

    @Test fun deleteWordBack() {
        judo.setInput("word word2 word3", 16)

        judo.type(keys("<esc>b"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 11)

        judo.type(keys("db"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word3" to 5)
    }

}

