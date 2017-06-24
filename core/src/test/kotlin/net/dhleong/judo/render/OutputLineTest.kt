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

        val (first, second) = line.getDisplayOutputLines(6, wordWrap = false)
        assertThat(first.toAttributedString().toAnsi()).isEqualTo("${ansi(1,2)}Take m${ansi(0)}")
        assertThat(second.toAttributedString().toAnsi()).isEqualTo("${ansi(1,2)}y love${ansi(0)}")

    }

    @Test(timeout = 3000) fun wordWrap() {
        val line = OutputLine("Take my love, take my land")
        assertThat(line.getWrappedStrings(10))
            .containsExactly("Take my ", "love, take", " my land")
    }

    @Test(timeout = 3000) fun wordWrapSuper() {
        // just don't spin-loop forever
        val line = OutputLine(" at net.dhleong.judo.modes.PythonCmdMode$\$special$\$inlined\$forEach\$lambda$1.__call__(PythonCmdMode.kt:308)")
        assertThat(line.getWrappedStrings(10))
            .startsWith(
                " at ",
                "net.dhleon",
                "g.judo.mod"
            )
    }

    @Test fun wordWrapEmptyLine() {
        val line = OutputLine("")
        assertThat(line.getWrappedStrings(10))
            .containsExactly("")
    }

    private fun OutputLine.getWrappedStrings(windowWidth: Int) =
        getDisplayLines(windowWidth, wordWrap = true)
            .map { it.toString() }
}