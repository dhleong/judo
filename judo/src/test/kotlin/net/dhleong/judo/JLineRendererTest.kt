package net.dhleong.judo

import org.assertj.core.api.Assertions.assertThat
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
}

fun JLineRenderer.appendOutput(string: String) {
    appendOutput(string.toCharArray(), string.length)
}