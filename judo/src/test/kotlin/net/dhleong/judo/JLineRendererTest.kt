package net.dhleong.judo

import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

/**
 * @author dhleong
 */
class JLineRendererTest {
    @Test fun appendOutput() {
        val renderer = JLineRenderer()

        renderer.appendOutput("\r\nTake my love,\r\n\r\nTake my land,\r\nTake me")

        assertThat(renderer.getOutputLines())
            .containsExactly(
                "",
                "Take my love,",
                "",
                "Take my land,",
                "Take me"
            )
        assertThat(renderer.getScrollbackTop()).isEqualTo(0)
    }

    @Test fun appendOutput_newlineOnly() {
        val renderer = JLineRenderer()

        renderer.appendOutput("\nTake my love,\nTake my land,\n\nTake me")

        assertThat(renderer.getOutputLines())
            .containsExactly(
                "",
                "Take my love,",
                "Take my land,",
                "",
                "Take me"
            )
        assertThat(renderer.getScrollbackTop()).isEqualTo(0)
    }

    @Test fun appendOutput_fancy() {
        val renderer = JLineRenderer()

        renderer.appendOutput(
            "\n\r${0x27}[1;30m${0x27}[1;37mTake my love," +
            "\n\r${0x27}[1;30m${0x27}[1;37mTake my land,")

        assertThat(renderer.getOutputLines())
            .containsExactly(
                "",
                "${0x27}[1;30m${0x27}[1;37mTake my love,",
                "${0x27}[1;30m${0x27}[1;37mTake my land,"
            )
        assertThat(renderer.getScrollbackTop()).isEqualTo(0)
    }

    @Test
    @Ignore("TODO")
    fun appendOutput_resumePartial() {
        // append without a line end and continue that line
        // in a separate append. It's TCP so stuff happens
        val renderer = JLineRenderer()

        renderer.appendOutput("\n\rTake my love,\n\rTake my")
        renderer.appendOutput(" land,\n\rTake me where...\n\r")
        renderer.appendOutput("I don't care, I'm still free")

        assertThat(renderer.getOutputLines())
            .containsExactly(
                "",
                "Take my love,",
                "Take my land,",
                "Take me where...",
                "I don't care, I'm still free"
            )
        assertThat(renderer.getScrollbackTop()).isEqualTo(0)
    }
}

fun JLineRenderer.appendOutput(string: String) {
    appendOutput(string.toCharArray(), string.length)
}