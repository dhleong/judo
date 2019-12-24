package net.dhleong.judo.motions

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import net.dhleong.judo.input.keys
import net.dhleong.judo.setInput
import net.dhleong.judo.type
import org.junit.Test

/**
 * TODO refactor into multiple, separate files
 *
 * @author dhleong
 */
class MotionIntegrationTest : AbstractMotionIntegrationTest() {

    @Test fun altBackSpace() {
        judo.setInput("word word2 word3 word4", 22)

        judo.type(keys("i<alt bs>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3 " to 17)

        judo.buffer.cursor = 11
        judo.type(keys("<alt bs>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word3 " to 5)

        judo.type(keys("<alt bs>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word3 " to 0)

        // nop:
        judo.type(keys("<alt bs>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word3 " to 0)

        judo.type(keys("<esc>A<alt bs>"))
        assertThat(renderer.outputLines).isEmpty() // no error deleting last text
        assertThat(renderer.inputLine)
            .isEqualTo("" to 0)
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

    @Test fun moveCountFindForward() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("2f<space>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 10)
    }

    @Test fun moveWordEmpty() {
        judo.setInput("", 0)

        judo.type(keys("w"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("" to 0)

        judo.type(keys("b"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("" to 0)
    }

    @Test fun moveCharRight_butNotBeyond() {
        judo.setInput("word", 3)
        judo.type(keys("l"))
        assertThat(renderer.inputLine)
            .isEqualTo("word" to 3)
    }

    @Test fun moveToFirst() {
        judo.setInput("word word2 word3", 11)

        judo.type(keys("0"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 0)
    }

    @Test fun moveToEnd_butNotBeyond() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("$"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 15)

        judo.type(keys("ciw"))
        assertThat(renderer.outputLines).isEmpty() //  no error
    }


    @Test fun changeBackEmpty() {
        judo.setInput("", 0)

        judo.type(keys("chl"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("l" to 1) // no change
    }

    @Test fun changeForwardEmpty() {
        judo.setInput("", 0)

        judo.type(keys("clh"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("h" to 1) // no change
    }

    @Test fun changeWordForward() {
        judo.setInput("word word2 word3", 5)

        judo.type(keys("cwchanged2 "))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("word changed2 word3" to 14)
    }

    @Test fun changeEndOfWordForward() {
        judo.setInput("word word2 word3", 5)

        judo.type(keys("cechanged2"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("word changed2 word3" to 13)
    }

    @Test fun changeInnerWord_last() {
        judo.setInput("word word2 word3", 11)

        judo.type(keys("ciwchanged"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 changed" to 18)
    }

    @Test fun changeLine() {
        judo.setInput("word", 3)

        judo.type(keys("ccnewWord"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("newWord" to 7)
    }

    @Test fun changeToEnd_last() {
        judo.setInput("word w@rd2 word3", 11)

        judo.type(keys("Cchanged"))
        assertThat(renderer.inputLine)
            .isEqualTo("word w@rd2 changed" to 18)
    }

    @Test fun replaceChar() {
        judo.setInput("word word2 word3", 5)

        // ignore cancel things
        judo.type(keys("r<esc>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 5)

        judo.type(keys("r<ctrl c>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 5)

        // simple replace
        judo.type(keys("ra"))
        assertThat(renderer.inputLine)
            .isEqualTo("word aord2 word3" to 5)

        judo.type(keys("rb"))
        assertThat(renderer.inputLine)
            .isEqualTo("word bord2 word3" to 5)
    }

    @Test fun countReplaceChar() {
        judo.setInput("word word2 word3", 5)

        // ignore cancel things
        judo.type(keys("r<esc>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 5)

        judo.type(keys("r<ctrl c>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 5)

        // simple replace
        judo.type(keys("3ra"))
        assertThat(renderer.inputLine)
            .isEqualTo("word aaad2 word3" to 5)

        judo.type(keys("rb"))
        assertThat(renderer.inputLine)
            .isEqualTo("word baad2 word3" to 5)
    }

    @Test fun flipCase() {
        judo.setInput("Wo&Rd", 0)

        // ignore cancel things
        judo.type(keys("~~~~"))
        assertThat(renderer.inputLine)
            .isEqualTo("wO&rd" to 4)

        judo.type(keys("~"))
        assertThat(renderer.inputLine)
            .isEqualTo("wO&rD" to 4) // stay at end
    }

    @Test fun flipCase_empty() {
        judo.setInput("", 0)

        judo.type(keys("~"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("" to 0)
    }

    @Test fun flipCase_innerWord() {
        judo.setInput("Wo&Rd", 0)

        // ignore cancel things
        judo.type(keys("g~iw"))
        assertThat(renderer.inputLine)
            .isEqualTo("wO&Rd" to 0)
    }

    @Test fun flipCase_WORDback() {
        judo.setInput("Wo&Rd", 4)

        // ignore cancel things
        judo.type(keys("g~B"))
        assertThat(renderer.outputLines).isEmpty() // no error
        assertThat(renderer.inputLine)
            .isEqualTo("wO&rd" to 0)
    }

    @Test fun goLower_word() {
         judo.setInput("WORD WORD2 WORD3", 5)

        judo.type(keys("guw"))
        assertThat(renderer.inputLine)
            .isEqualTo("WORD word2 WORD3" to 5)
    }

    @Test fun repeatFind() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("fo"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 1)

        judo.type(keys(";"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 6)

        // go back...
        judo.type(keys(","))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 1)

        // and forward twice
        judo.type(keys(";;"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 12)
    }

    @Test fun repeatTil() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("tr"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 1)

        judo.type(keys(";"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 6)

        // go back...
        judo.type(keys(","))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 3)

        // and forward twice
        judo.type(keys(";;"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 12)
    }
}

