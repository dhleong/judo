package net.dhleong.judo.util

import net.dhleong.judo.input.InputBuffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class InputHistoryTest {

    val buffer = InputBuffer()
    val history = InputHistory(buffer)

    @Before fun setUp() {
        buffer.clear()
        history.clear()
    }

    @Test fun scrollHistory() {
        history.push("Take me where I cannot stand")
        history.push("Take my land")
        history.push("Take my love")
        buffer.set("I don't care")
        assertThat(buffer.toString()).isEqualTo("I don't care")

        history.scroll(-1)
        assertThat(buffer.toString()).isEqualTo("Take my love")

        history.scroll(-1)
        assertThat(buffer.toString()).isEqualTo("Take my land")

        history.scroll(-1)
        assertThat(buffer.toString()).isEqualTo("Take me where I cannot stand")

        // no more history; should be the same
        history.scroll(-1)
        assertThat(buffer.toString()).isEqualTo("Take me where I cannot stand")

        // go back forward
        history.scroll(1)
        assertThat(buffer.toString()).isEqualTo("Take my land")

        history.scroll(1)
        assertThat(buffer.toString()).isEqualTo("Take my love")

        // finally, restore the "last buffer"
        history.scroll(1)
        assertThat(buffer.toString()).isEqualTo("I don't care")

        // no change
        history.scroll(1)
        assertThat(buffer.toString()).isEqualTo("I don't care")
    }
}