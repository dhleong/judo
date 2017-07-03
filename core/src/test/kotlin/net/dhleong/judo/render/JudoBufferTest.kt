package net.dhleong.judo.render

import net.dhleong.judo.util.ansi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class JudoBufferTest {

    lateinit var buffer: JudoBuffer

    @Before fun setUp() {
        buffer = JudoBuffer(IdManager())
    }

    @Test fun appendOutput_empty() {

        // basically make sure it doesn't crash
        buffer.appendLine("", isPartialLine = true)
        buffer.appendLine("", isPartialLine = false)

        assertThat(buffer.getAnsiContents())
            .containsExactly("")
    }

    @Test fun appendOutput_resumePartial() {
        // append without a line end and continue that line
        // in a separate append. It's TCP so stuff happens

        buffer.appendLine("", isPartialLine = false)
        buffer.appendLine("Take my love,", isPartialLine = false)
        buffer.appendLine("Take my", isPartialLine = true)
        buffer.appendLine(" land,", isPartialLine = false)
        buffer.appendLine("Take me where...", isPartialLine = false)
        buffer.appendLine("I don't care, I'm still free", isPartialLine = false)

        assertThat(buffer.getAnsiContents())
            .containsExactly(
                "",
                "Take my love,",
                "Take my land,",
                "Take me where...",
                "I don't care, I'm still free"
            )
    }

    @Test fun appendOutput_resumePartial_continueAnsi() {
        // append without a line end and continue that line
        // in a separate append. It's TCP so stuff happens

        buffer.appendLine("", isPartialLine = false)
        buffer.appendLine("${ansi(1,6)}Take my ", isPartialLine = true)
        buffer.appendLine("love", isPartialLine = false)

        assertThat(buffer.getAnsiContents())
            .containsExactly(
                "",
                "${ansi(1,6)}Take my love${ansi(0)}"
            )
    }

    @Test fun appendOutput_resumePartial_continueAnsi2() {

        // continuation of the partial line has its own ansi;
        // use previous line's ansi to start, but don't stomp
        // on the new ansi
        val first = OutputLine("${ansi(1,6)}Take my ")
        val second = OutputLine("lo${ansi(1,7)}ve")
        buffer.appendLine(first, isPartialLine = true)
        buffer.appendLine(second, isPartialLine = false)

        assertThat(buffer.getAnsiContents()).hasSize(1)
        assertThat(buffer.getAnsiContents()[0])
            .isEqualTo(
                "${ansi(1,6)}Take my lo${ansi(fg=7)}ve${ansi(0)}"
            )
    }

    @Test fun appendOutput_resumePartial_trailingAnsi() {
        // append without a line end and continue that line
        // in a separate append. It's TCP so stuff happens

        val trailingAnsiLine = OutputLine("${ansi(1,6)}Take my ${ansi(1,2)}")
        buffer.appendLine("", isPartialLine = false)
        buffer.appendLine(trailingAnsiLine, isPartialLine = true)
        buffer.appendLine("love", isPartialLine = false)

        assertThat(buffer.getAnsiContents())
            .containsExactly(
                "",
                "${ansi(1,6)}Take my ${ansi(fg=2)}love${ansi(0)}"
            )
    }

    @Test fun appendOutput_resumePartial_splitAnsi() {
        // append without a line end and continue that line
        // in a separate append. It's TCP so stuff happens

        val ansi = ansi(1,2)
        val firstHalf = ansi.slice(0..3)
        val secondHalf = ansi.slice(4..ansi.lastIndex)
        assertThat("$firstHalf$secondHalf").isEqualTo(ansi.toString())

        buffer.appendLine(
            OutputLine("${ansi(1,6)}Take my $firstHalf"),
            isPartialLine = true)
        buffer.appendLine(
            OutputLine("${secondHalf}love"),
            isPartialLine = false)

        assertThat(buffer.getAnsiContents())
            .containsExactly(
                "${ansi(1,6)}Take my ${ansi(fg=2)}love${ansi(0)}"
            )
    }


}

fun JudoBuffer.getAnsiContents(): List<String> =
    (0..this.lastIndex).map { (this[it] as OutputLine).toAttributedString().toAnsi() }

private fun JudoBuffer.appendLine(line: CharSequence, isPartialLine: Boolean) =
    appendLine(line, isPartialLine, windowWidthHint = 40, wordWrap = false)
