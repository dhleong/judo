package net.dhleong.judo

import net.dhleong.judo.util.ansi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

/**
 * @author dhleong
 */
class JLineRendererTest {
    // TODO test splitting lines
    // TODO test scrollback

    @Test fun appendOutput_resumePartial() {
        // append without a line end and continue that line
        // in a separate append. It's TCP so stuff happens
        val renderer = JLineRenderer()
        renderer.windowWidth = 42

        renderer.appendOutput("", isPartialLine = false)
        renderer.appendOutput("Take my love,", isPartialLine = false)
        renderer.appendOutput("Take my", isPartialLine = true)
        renderer.appendOutput(" land,", isPartialLine = false)
        renderer.appendOutput("Take me where...", isPartialLine = false)
        renderer.appendOutput("I don't care, I'm still free", isPartialLine = false)

        assertThat(renderer.getOutputLines())
            .containsExactly(
                "",
                "Take my love,",
                "Take my land,",
                "Take me where...",
                "I don't care, I'm still free"
            )
        assertThat(renderer.getScrollback()).isEqualTo(0)
    }

    @Ignore("FIXME: ansi codes list between split lines")
    @Test fun appendOutput_resumePartial_fancy() {
        // append without a line end and continue that line
        // in a separate append. It's TCP so stuff happens
        val renderer = JLineRenderer()
        renderer.windowWidth = 42

        renderer.appendOutput("", isPartialLine = false)
        renderer.appendOutput("${ansi(1,36)}Take my ${ansi(1,32)}", isPartialLine = true)
        renderer.appendOutput("love", isPartialLine = false)

        assertThat(renderer.getOutputLines())
            .containsExactly(
                "",
                "${ansi(1,36)}Take my ${ansi(1,32)}love"
            )
        assertThat(renderer.getScrollback()).isEqualTo(0)
    }

}

