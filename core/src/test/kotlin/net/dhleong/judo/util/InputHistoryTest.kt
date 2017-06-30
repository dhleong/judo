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
        assertThat(buffer.cursor).isEqualTo(11)

        history.scroll(-1)
        assertThat(buffer.toString()).isEqualTo("Take my land")

        history.scroll(-1)
        assertThat(buffer.toString()).isEqualTo("Take me where I cannot stand")
        assertThat(buffer.cursor).isEqualTo(27)

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

    @Test fun scrollHistoryUnclamped() {
        history.push("Take me where I cannot stand")
        history.push("Take my land")
        history.push("Take my love")
        buffer.set("I don't care")
        assertThat(buffer.toString()).isEqualTo("I don't care")

        history.scroll(-1, clampCursor = false)
        assertThat(buffer.toString()).isEqualTo("Take my love")
        assertThat(buffer.cursor).isEqualTo(12)

        history.scroll(-2, clampCursor = false)
        assertThat(buffer.toString()).isEqualTo("Take me where I cannot stand")
        assertThat(buffer.cursor).isEqualTo(28)
    }

    @Test fun searchHistory() {
        history.push("Take me where I cannot stand")
        history.push("Take my land")
        history.push("Take my love")
        assertThat(buffer.toString()).isEqualTo("")

        assertThat(history.search("t", false)).isTrue()
        assertThat(buffer.toString()).isEqualTo("Take my love")

        assertThat(history.search("ta", false)).isTrue()
        assertThat(buffer.toString()).isEqualTo("Take my love")

        // no match? no change
        assertThat(history.search("tar", false)).isFalse()
        assertThat(buffer.toString()).isEqualTo("Take my love")

        assertThat(history.search("ta", true)).isTrue()
        assertThat(buffer.toString()).isEqualTo("Take my land")

        assertThat(history.search("take", false)).isTrue()
        assertThat(buffer.toString()).isEqualTo("Take my land")

        assertThat(history.search("take me", false)).isTrue()
        assertThat(buffer.toString()).isEqualTo("Take me where I cannot stand")

        // no change even though we force
        assertThat(history.search("take me", true)).isFalse()
        assertThat(buffer.toString()).isEqualTo("Take me where I cannot stand")
    }

    @Test fun searchAfterScroll() {
        history.push("Take me where I cannot stand")
        history.push("Take my land")
        history.push("Take my love")

        history.scroll(-1)
        assertThat(buffer.toString()).isEqualTo("Take my love")

        assertThat(history.search("t", false)).isTrue()
        assertThat(buffer.toString()).isEqualTo("Take my land")
    }

    @Test fun dontDupHistory() {
        history.push("Take me where I cannot stand")
        history.push("Take me where I cannot stand")
        history.push("Take me where I cannot stand")
        history.push("Take my land")
        history.push("Take my land")
        history.push("Take my land")
        history.push("Take my love")
        history.push("Take my land")

        // NOTE: it's fine to not search back through
        // the entire history, but we should at least
        // not repeat the most recent
        assertThat(history.size).isEqualTo(4)
    }

    @Test fun searchHistoryPastDups() {
        history.push("Take my land")
        history.push("Take my love")
        history.push("Take me where I cannot stand")
        history.push("Take my love")
        assertThat(buffer.toString()).isEqualTo("")

        assertThat(history.search("take my", false)).isTrue()
        assertThat(buffer.toString()).isEqualTo("Take my love")

        assertThat(history.search("take my", true)).isTrue()
        assertThat(buffer.toString()).isEqualTo("Take my land")

        // and, as usual, stay put at the end
        assertThat(history.search("take my", true)).isFalse()
        assertThat(buffer.toString()).isEqualTo("Take my land")
    }
}

