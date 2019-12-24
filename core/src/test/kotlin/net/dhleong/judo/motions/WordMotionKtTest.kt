package net.dhleong.judo.motions

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.coroutines.runBlocking
import net.dhleong.judo.DUMMY_JUDO_CORE
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.type
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class WordMotionKtTest {
    val buffer = InputBuffer()

    @Before fun setUp() {
        buffer.clear()
    }

    @Test fun moveWord_space() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type("captain malcolm    reynolds    ")
        buffer.cursor = 0

        wordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(8)

        wordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(19)

        wordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(30)

        wordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(30)
    }

    @Test fun moveWord_spaceBack() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type("malcolm reynolds    ")
        buffer.cursor = 20
        assertThat(buffer.cursor).isEqualTo(20)

        wordMotion(-1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(8)

        wordMotion(-1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(0)

        wordMotion(-1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(0)
    }

    @Test fun moveWord_special() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type("malcolm(reynold's)")
        buffer.cursor = 0
        wordMotion(1, false).applyTo(buffer)

        assertThat(buffer.cursor).isEqualTo(7)

        wordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(8)

        wordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(15)
    }

    @Test fun moveWord_specialBack() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type("malcolm(reynold's)  ")
        buffer.cursor = 19
        wordMotion(-1, false).applyTo(buffer)

        assertThat(buffer.cursor).isEqualTo(17)

        wordMotion(-1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(16)

        wordMotion(-1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(15)
        assertThat(buffer.toChars()[buffer.cursor]).isEqualTo('\'')

        wordMotion(-1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(8)
    }


    @Test fun moveWordEnd_space() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type("captain malcolm    reynolds    ")
        buffer.cursor = 0

        endOfWordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(6)

        endOfWordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(14)

        endOfWordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(26)

        endOfWordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(30)
    }

    @Test fun moveWordEnd_spaceBack() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type("malcolm reynolds    ")
        buffer.cursor = 20

        endOfWordMotion(-1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(15)

        endOfWordMotion(-1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(6)

        endOfWordMotion(-1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(0)

        endOfWordMotion(-1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(0)
    }

    @Test fun moveWordEnd_special() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type("malcolm(reynold's)")
        buffer.cursor = 0
        endOfWordMotion(1, false).applyTo(buffer)

        assertThat(buffer.cursor).isEqualTo(6)

        endOfWordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(7)

        endOfWordMotion(1, false).applyTo(buffer)
        assertThat(buffer.cursor).isEqualTo(14)
    }
}

private fun Motion.applyTo(buffer: InputBuffer) = runBlocking {
    applyTo(DUMMY_JUDO_CORE, buffer)
}

