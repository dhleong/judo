package net.dhleong.judo

import net.dhleong.judo.net.JudoConnection
import net.dhleong.judo.render.OutputLine
import net.dhleong.judo.util.InputHistory
import net.dhleong.judo.util.ansi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * @author dhleong
 */
class JudoCoreTest {

    var renderer = TestableJudoRenderer()
    lateinit var judo: JudoCore

    @Before fun setUp() {
        renderer.output.clear()
        judo = JudoCore(renderer, renderer.mapRenderer, StateMap())
    }

    @Test fun appendOutput() {
        judo.appendOutput("\r\nTake my love,\r\n\r\nTake my land,\r\nTake me")

        assertThat(renderer.outputLines)
            .containsExactly(
                "",
                "Take my love,",
                "",
                "Take my land,",
                "Take me"
            )
    }

    @Test fun appendOutput_newlineOnly() {
        judo.appendOutput("\nTake my love,\nTake my land,\n\nTake me")

        assertThat(renderer.outputLines)
            .containsExactly(
                "",
                "Take my love,",
                "Take my land,",
                "",
                "Take me"
            )
    }

    @Test fun appendOutput_fancy() {

        judo.appendOutput(
            "\n\r${0x27}[1;30m${0x27}[1;37mTake my love," +
                "\n\r${0x27}[1;30m${0x27}[1;37mTake my land,")

        assertThat(renderer.outputLines)
            .containsExactly(
                "",
                "${0x27}[1;30m${0x27}[1;37mTake my love,",
                "${0x27}[1;30m${0x27}[1;37mTake my land,"
            )
    }

    @Test fun appendOutput_midPartial() {
        judo.appendOutput("\n\rTake my love,\n\rTake my")
        judo.appendOutput(" land,\n\rTake me where...\n\r")
        judo.appendOutput("I don't care, I'm still free")

        assertThat(renderer.outputLines)
            .containsExactly(
                "",
                "Take my love,",
                "Take my land,",
                "Take me where...",
                "I don't care, I'm still free"
            )
    }

    @Test fun buildPromptWithAnsi() {
        val prompt = "${ansi(1,3)}HP: ${ansi(1,6)}42"
        judo.onPrompt(0, prompt)
        renderer.settableWindowWidth = 12

        val status = judo.buildStatusLine(fakeMode("Test"))

        assertThat(status.toAnsiString()).isEqualTo(
            "${ansi(1,3)}HP: ${ansi(fg = 6)}42${ansi(attr = 0)}[TEST]"
        )
    }

    @Test fun feedKeys() {
        judo.buffer.set("my love,")
        judo.buffer.cursor = 0

        judo.feedKeys("ITake")
        assertThat(judo.buffer.toString()).isEqualTo("Takemy love,")

        // still in insert mode
        judo.feedKeys(" ")
        assertThat(judo.buffer.toString()).isEqualTo("Take my love,")

        // now, feedKeys in normal mode
        judo.feedKeys("df,", mode = "normal")
        assertThat(judo.buffer.toString()).isEqualTo("Take ")

        // should have returned to insert mode
        judo.feedKeys(" my land")
        assertThat(judo.buffer.toString()).isEqualTo("Take my land ")
    }

    @Test fun readPersistedInput() {
        val tmpFile = File.createTempFile("judo-history-test", ".tmp")
        tmpFile.writeText("Take my love,\nTake my land")
        val history = with(JudoCore::class.java.getDeclaredField("sendHistory")) {
            isAccessible = true
            get(judo) as InputHistory
        }

        assertThat(history.size).isEqualTo(0)

        judo.persistInput(tmpFile)

        assertThat(history.size).isEqualTo(2)
        tmpFile.deleteOnExit()
    }

    @Test fun persistInput() {
        val tmpFile = File.createTempFile("judo-history-test", ".tmp")
        tmpFile.delete()
        val history = with(JudoCore::class.java.getDeclaredField("sendHistory")) {
            isAccessible = true
            get(judo) as InputHistory
        }

        assertThat(history.size).isEqualTo(0)

        judo.persistInput(tmpFile)
        assertThat(history.size).isEqualTo(0) // nothing there

        judo.send("take love", fromMap = false)
        judo.send("take land", fromMap = false)
        assertThat(history.size).isEqualTo(2)

        judo.onDisconnect(Proxy<JudoConnection>())

        assertThat(tmpFile)
            .exists()
            .hasContent("take love\ntake land")

        tmpFile.deleteOnExit()
    }

    @Test fun resetCompletionOnModeChange() {
        // seed completion
        judo.seedCompletion("take love")

        judo.feedKeys("itake l<tab>")
        assertThat(renderer.outputLines).isEmpty() // no errors
        assertThat(renderer.inputLine)
            .isEqualTo("take love" to 9)

        judo.feedKeys("<esc>ddi<tab>")

        assertThat(renderer.outputLines).isEmpty() // no errors
        assertThat(renderer.inputLine)
            .isEqualTo("take" to 4)
    }

    @Test fun triggerScriptingError() {
        judo.executeScript(
            """
            import re
            @trigger(re.compile("(take.*)"))
            def my_trigger(my, love): echo('triggered!')
            """.trimIndent())

        val buffer = "take my love\r\n".toCharArray()
        judo.onIncomingBuffer(buffer, buffer.size)
        assertThat(renderer.outputLines)
            .contains("TypeError: my_trigger() takes exactly 2 arguments (1 given)")
    }
}

private fun JudoCore.appendOutput(buffer: String) =
    appendOutput(OutputLine(buffer))

private fun fakeMode(name: String): Mode =
    object : Mode by Proxy() {
        override val name = name
    }
