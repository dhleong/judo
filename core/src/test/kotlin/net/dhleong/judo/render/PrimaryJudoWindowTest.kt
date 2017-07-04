package net.dhleong.judo.render

import net.dhleong.judo.StateMap
import net.dhleong.judo.assertThat
import org.junit.Test

/**
 * @author dhleong
 */
class PrimaryJudoWindowTest {
    private val ids = IdManager()
    private val settings = StateMap()
    private val outputBuffer = JudoBuffer(ids)
    private val primaryWindow = PrimaryJudoWindow(
        ids, settings, outputBuffer,
        -1, -1)

    @Test fun resize() {
        primaryWindow.resize(20, 5)

        assertThat(primaryWindow.outputWindow).hasHeight(4)
        assertThat(primaryWindow.promptWindow).hasHeight(1)

        primaryWindow.isFocused = true
        primaryWindow.appendLine("Take my love", isPartialLine = false)
        primaryWindow.appendLine("Take my land", isPartialLine = false)
        primaryWindow.updateStatusLine("<status>")

        assertThat(primaryWindow)
            .displaysStrings(
                "",
                "",
                "Take my love",
                "Take my land",
                "<status>"
            )
    }
}

