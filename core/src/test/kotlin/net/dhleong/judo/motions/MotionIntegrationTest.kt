package net.dhleong.judo.motions

import net.dhleong.judo.JudoCore
import net.dhleong.judo.StateMap
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.input.keys
import net.dhleong.judo.render.getAnsiContents
import net.dhleong.judo.setInput
import net.dhleong.judo.type
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * TODO refactor into multiple, separate files
 *
 * @author dhleong
 */
class MotionIntegrationTest {

    val renderer = TestableJudoRenderer()
    lateinit var judo: JudoCore

    @Before fun setUp() {
        judo = JudoCore(renderer, StateMap())
    }

    @After fun tearDown() {
        // if not empty, it contained errors
        assertThat(renderer.output.getAnsiContents()).isEmpty()
    }

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

    @Test fun deleteOuterQuotes() {
        judo.setInput("word \"word2\" word3", 8)

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

    @Test fun changeToEnd_last() {
        judo.setInput("word w@rd2 word3", 11)

        judo.type(keys("Cchanged"))
        assertThat(renderer.inputLine)
            .isEqualTo("word w@rd2 changed" to 18)
    }

    @Test fun deleteToEnd_last() {
        judo.setInput("word w@rd2 word3", 11)

        judo.type(keys("D"))
        assertThat(renderer.inputLine)
            .isEqualTo("word w@rd2 " to 10)
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

