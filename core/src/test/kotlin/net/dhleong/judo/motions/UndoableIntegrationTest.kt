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

    @Test fun redoDeleteWithCount() {
        judo.setInput("word word2 word3", 0)
        judo.type(keys("5x<esc>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word2 word3" to 0)

        judo.type(keys("u\$"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 15)

        judo.type(keys("<ctrl r>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word2 word3" to 0)
    }

    @Test fun redoDeleteWithRegister() {
        judo.setInput("word word2 word3", 0)
        judo.type(keys("\"xdwx\"xP"))
        assertThat(renderer.inputLine)
            .isEqualTo("word ord2 word3" to 5)
        assertThat(judo.registers.unnamed.value.toString()).isEqualTo("w")
        assertThat(judo.registers['x'].value.toString())
            .isEqualTo("word ")

        judo.type(keys("u\$"))
        assertThat(renderer.inputLine)
            .isEqualTo("ord2 word3" to 9)

        // NOTE: if we didn't include the register in the redo,
        // it would be pasting `w`, the current contents of
        // the unnamed register
        judo.type(keys("<ctrl r>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word ord2 word3" to 5)
    }

    @Test fun redoDelete() {
        judo.setInput("word word2 word3", 0)
        judo.type(keys("df <esc>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word2 word3" to 0)

        judo.type(keys("u\$"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 15)

        judo.type(keys("<ctrl r>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word2 word3" to 0)

        // one more time to ensure we don't overflow the stack
        judo.type(keys("u<ctrl r>"))
        assertThat(renderer.outputLines).isEmpty()
        assertThat(renderer.inputLine)
            .isEqualTo("word2 word3" to 0)
    }

    @Test fun redoDeleteWord() {
        judo.setInput("The quick red fox", 17)
        judo.type(keys("bbdaw"))
        assertThat(renderer.inputLine)
            .isEqualTo("The quick fox" to 10)

        judo.type(keys("u0"))
        assertThat(renderer.inputLine)
            .isEqualTo("The quick red fox" to 0)

        judo.type(keys("<ctrl r>"))
        assertThat(renderer.inputLine)
            .isEqualTo("The quick fox" to 10)

        judo.type(keys("u"))
        assertThat(renderer.inputLine)
            .isEqualTo("The quick red fox" to 10)

        judo.type(keys("<ctrl r>"))
        assertThat(renderer.inputLine)
            .isEqualTo("The quick fox" to 10)
    }

    @Test fun redoChange() {
        judo.setInput("word word2 word3", 0)
        judo.type(keys("cfdnew<esc>"))
        assertThat(renderer.inputLine)
            .isEqualTo("new word2 word3" to 2)

        judo.type(keys("u\$"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 15)

        judo.type(keys("<ctrl r>"))
        assertThat(renderer.inputLine)
            .isEqualTo("new word2 word3" to 2)

        // one more time to ensure we don't overflow the stack
        judo.type(keys("u<ctrl r>"))
        assertThat(renderer.outputLines).isEmpty()
        assertThat(renderer.inputLine)
            .isEqualTo("new word2 word3" to 2)
    }

    @Test fun redoInsert() {
        judo.setInput("word word2 word3", 0)
        judo.type(keys("Iword0 <esc>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word0 word word2 word3" to 5)

        judo.type(keys("u\$"))
        assertThat(renderer.inputLine)
            .isEqualTo("word word2 word3" to 15)

        judo.type(keys("<ctrl r>"))
        assertThat(renderer.inputLine)
            .isEqualTo("word0 word word2 word3" to 5)

        // one more time to ensure we don't overflow the stack
        judo.type(keys("u<ctrl r>"))
        assertThat(renderer.outputLines).isEmpty()
        assertThat(renderer.inputLine)
            .isEqualTo("word0 word word2 word3" to 5)
    }

    @Test fun repeatChange() {
        judo.setInput("word word2 word3", 0)

        judo.type(keys("cfdfun<esc>"))
        assertThat(renderer.inputLine)
            .isEqualTo("fun word2 word3" to 2)

        judo.type(keys("w.w."))
        assertThat(renderer.outputLines).isEmpty() // no errors
        assertThat(renderer.inputLine)
            .isEqualTo("fun fun2 fun3" to 11)
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

    @Test fun startInsert() {
        judo.type(keys(":startInsert()<cr>"))
        judo.type(keys("serenity<esc>"))
        assertThat(renderer.outputLines).isEmpty() // no errors
        assertThat(renderer.inputLine)
            .isEqualTo("serenity" to 7)
    }
}