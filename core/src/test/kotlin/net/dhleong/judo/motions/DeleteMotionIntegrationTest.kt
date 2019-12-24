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
class DeleteMotionIntegrationTest : AbstractMotionIntegrationTest() {

    @Test fun deleteEndOfWordBack() {
        judo.setInput("word word2 word3", 9)

        judo.type(keys("dge"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("word2 word3" to 4)
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

    @Test fun deleteCountFindForward() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("d2f<space>"))
        assertThat(renderer.outputLines).isEmpty() // no errors
        assertThat(renderer.inputLine)
            .isEqualTo("word3" to 0)
    }

    @Test fun countDeleteFindForward() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("2df<space>"))
        assertThat(renderer.outputLines).isEmpty() // no errors
        assertThat(renderer.inputLine)
            .isEqualTo("word3" to 0)
    }

    @Test fun deleteLine() {
        judo.setInput("word word2 word3", 11)

        judo.type(keys("dd"))
        assertThat(renderer.outputLines).isEmpty()
        assertThat(renderer.inputLine)
            .isEqualTo("" to 0)
    }

    @Test fun deleteInnerWord() {
        judo.setInput("word word2 word3", 11)

        judo.type(keys("diw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 " to 10)
    }

    @Test fun deleteInnerWord_empty() {
        judo.setInput("", 0)

        judo.type(keys("diw"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("" to 0)
    }

    @Test fun deleteInnerWord_symbols() {
        judo.setInput("word w@rd2 word3", 5)

        judo.type(keys("diw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word @rd2 word3" to 5)

        judo.type(keys("diw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word rd2 word3" to 5)
    }

    @Test fun deleteCountInnerWord_2() {
        judo.setInput("word w@rd2 word3", 5)

        judo.type(keys("d2iw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word rd2 word3" to 5)
    }

    @Test fun deleteCountInnerWord_4() {
        judo.setInput("word w@rd2 word3", 5)

        judo.type(keys("d4iw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word3" to 5)
    }

    @Test fun deleteOuterWord() {
        judo.setInput("word word2 word3", 5)

        judo.type(keys("daw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word3" to 5)
    }

    @Test fun deleteOuterWord_last() {
        judo.setInput("word word2 word3", 11)

        judo.type(keys("daw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2" to 9)
    }

    @Test fun deleteOuterWord_symbols() {
        judo.setInput("word w@rd2 word3", 5)

        judo.type(keys("daw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word@rd2 word3" to 4)
    }

    @Test fun deleteOuterWORD() {
        judo.setInput("word w@rd2 word3", 5)

        judo.type(keys("daW"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word3" to 5)
    }

    @Test fun deleteOuterWORD_last() {
        judo.setInput("word w@rd2 word3", 11)

        judo.type(keys("daW"))
        assertThat(renderer.inputLine)
            .isEqualTo("word w@rd2" to 9)
    }


    @Test fun deleteToEnd_last() {
        judo.setInput("word w@rd2 word3", 11)

        judo.type(keys("D"))
        assertThat(renderer.inputLine)
            .isEqualTo("word w@rd2 " to 10)
    }

    @Test fun deleteToEnd_lastChar() {
        judo.setInput("word w@rd2 word3", 15)

        judo.type(keys("D"))
        assertThat(renderer.inputLine)
            .isEqualTo("word w@rd2 word" to 14)
    }

    @Test fun deleteMotionToEnd_lastChar() {
        judo.setInput("word w@rd2 word3", 15)

        judo.type(keys("d$"))
        assertThat(renderer.inputLine)
            .isEqualTo("word w@rd2 word" to 14)
    }

    @Test fun deleteUntilBack() {
        judo.setInput("word word2 word3", 11)

        judo.type(keys("dTw"))
        assertThat(renderer.inputLine)
            .isEqualTo("word wword3" to 6)
    }

    @Test fun deleteUntilForward() {
        judo.setInput("word word2 word3", 5)

        judo.type(keys("dt<space>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word  word3" to 5)
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

    @Test fun deleteCountWordBack() {
        judo.setInput("word word2 word3", 16)

        judo.type(keys("<esc>b"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 11)

        judo.type(keys("d2b"))
        assertThat(renderer.inputLine)
            .isEqualTo("word3" to 0)
    }
}