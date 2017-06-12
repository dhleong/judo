package net.dhleong.judo.render

import net.dhleong.judo.util.ansi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * @author dhleong
 */
class OutputLineTest {
    @Test fun dontLoseStyleWhenSplitting() {
        val line = OutputLine("Take my love")
        line.setStyleHint(ansi(1,2))
        assertThat(line.toAnsi()).isEqualTo("${ansi(1,2)}Take my love")

        val (first, second) = line.getDisplayOutputLines(6)
        assertThat(first.toAttributedString().toAnsi()).isEqualTo("${ansi(1,2)}Take m${ansi(0)}")
        assertThat(second.toAttributedString().toAnsi()).isEqualTo("${ansi(1,2)}y love${ansi(0)}")

    }
}