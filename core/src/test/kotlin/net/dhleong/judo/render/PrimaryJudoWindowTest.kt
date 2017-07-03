package net.dhleong.judo.render

import net.dhleong.judo.StateMap
import org.assertj.core.api.Assertions.assertThat
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

        // TODO WindowAssertion ?
        assertThat(primaryWindow.outputWindow.height).isEqualTo(4)
        assertThat(primaryWindow.promptWindow.height).isEqualTo(1)

        primaryWindow.isFocused = true
        primaryWindow.appendLine("Take my love", isPartialLine = false)
        primaryWindow.appendLine("Take my land", isPartialLine = false)
        primaryWindow.updateStatusLine("<status>")

        assertThat(primaryWindow.getDisplayStrings())
            .containsExactly(
                "",
                "",
                "Take my love",
                "Take my land",
                "<status>"
            )
    }

}

fun IJudoWindow.getDisplayStrings(): List<String> =
    with(mutableListOf<CharSequence>()) {
        getDisplayLines(this)
        map { it.toString() }
    }
