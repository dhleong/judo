package net.dhleong.judo

import assertk.assert
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import com.nhaarman.mockito_kotlin.mock
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.modes.BlockingEchoMode
import net.dhleong.judo.modes.CmdMode
import net.dhleong.judo.modes.NormalMode
import net.dhleong.judo.net.AnsiFlavorableStringReader
import net.dhleong.judo.net.toAnsi
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.PrimaryJudoWindow
import net.dhleong.judo.render.SimpleFlavor
import net.dhleong.judo.render.toFlavorable
import net.dhleong.judo.util.ansi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * @author dhleong
 */
class JudoCoreTest {

    private var renderer = TestableJudoRenderer()
    private lateinit var judo: JudoCore

    @Before fun setUp() {
        renderer.output.clear()
        judo = JudoCore(
            renderer, renderer.mapRenderer, StateMap(),
            connections = DummyConnectionFactory
        )
    }

    @Test fun `print() handles newlines`() {
        judo.print("\nTake my love,\n\nTake my land,\nTake me")
        assertThat(renderer.outputLines)
            .containsExactly(
                "",
                "Take my love,",
                "",
                "Take my land,",
                "Take me"
            )
    }

    @Test fun `print() handles ansi codes in strings`() {
        judo.print("${ansi(1)}Mal ${ansi(3)}Reynolds")
        assert(renderer.outputLines).containsExactly(
            "Mal Reynolds"
        )
        assert(renderer.flavoredOutputLines).containsExactly(
            FlavorableStringBuilder(16).apply {
                append("Mal ", SimpleFlavor(isBold = true))
                append("Reynolds", SimpleFlavor(isBold = true, isItalic = true))
            }
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

    @Test fun `(appendOutput) no excessive newlines from CR`() {
        judo.appendOutput("Take my love, \r${0x27}[1;40m\nTake my land,\r${0x27}[1;40m")

        assertThat(renderer.outputLines)
            .containsExactly(
                "Take my love, \r${0x27}[1;40m",
                "Take my land,\r${0x27}[1;40m"
            )
    }

    @Test fun appendOutput_midPartial() {
        judo.appendOutput("\nTake my love,\nTake my")
        judo.appendOutput(" land,\nTake me where...\n")
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

    @Test fun `Catch split prompts`() {
        judo.prompts.define("HP: $1", "(hp: $1)")

        val prompt = "${ansi(1,3)}HP: ${ansi(1,6)}42\r\n"
        val first = prompt.substring(0..8)
        val second = prompt.substring(9..prompt.lastIndex)
        assert("$first$second").isEqualTo(prompt)

        val reader = AnsiFlavorableStringReader()
        val sequences = reader.feed(first.toCharArray()) +
            reader.feed(second.toCharArray())

        for (s in sequences) {
            judo.onIncomingBuffer(s)
        }

        val buffer = judo.renderer.currentTabpage.currentWindow.currentBuffer
        assert(buffer).hasLines(
            ""
        )
    }

    @Test fun buildPromptWithAnsi() {
        val prompt = "${ansi(1,3)}HP: ${ansi(1,6)}42"
        judo.onPrompt(0, prompt, 0)
        renderer.settableWindowWidth = 12

        val status = judo.buildStatusLine(
            createWindowMock(IdManager(), 12, 1, emptyBuffer()),
            fakeMode("Test")
        )

        assertThat(status.toAnsi()).isEqualTo(
            "${ansi(1,3)}HP: ${ansi(fg = 6)}42${ansi(attr = 0)}[TEST]"
        )
    }

    @Test fun `Multiple prompts in a group`() {
        val win = judo.renderer.currentTabpage.currentWindow as PrimaryJudoWindow
        val promptBuffer = win.promptWindow.currentBuffer

        judo.prompts.define("^HP[$1]>", "HP: $1", group = 1)
        judo.prompts.define("^Ammo: $1>", "Ammo: $1", group = 1)

        judo.prompts.define("^HP $1>", "<$1>")

        judo.onIncomingBuffer("HP 42>\n".toFlavorable())
        assert(promptBuffer).hasLines(
            "<42>\n"
        )

        judo.onIncomingBuffer("HP[42]>\n".toFlavorable())
        assert(promptBuffer).hasLines(
            "HP: 42\n"
        )

        judo.onIncomingBuffer("Ammo: 22>\n".toFlavorable())
        assert(promptBuffer).hasLines(
            "HP: 42\n",
            "Ammo: 22\n"
        )

        // back to group -1
        judo.onIncomingBuffer("HP 32>\n".toFlavorable())
        assert(promptBuffer).hasLines(
            "<32>\n"
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
            get(judo) as IInputHistory
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
            get(judo) as IInputHistory
        }

        assertThat(history.size).isEqualTo(0)

        judo.persistInput(tmpFile)
        assertThat(history.size).isEqualTo(0) // nothing there

        judo.submit("take love", fromMap = false)
        judo.submit("take land", fromMap = false)
        assertThat(history.size).isEqualTo(2)

        judo.onDisconnect(DumbProxy())

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
        judo.executeScript("""
            import re
            @trigger(re.compile("(take.*)"))
            def my_trigger(my, love): print('triggered!')
        """.trimIndent())

        val buffer = "take my love\r\n"
        judo.onIncomingBuffer(FlavorableStringBuilder.withDefaultFlavor(buffer))
        assertThat(renderer.outputLines)
            .contains("TypeError: my_trigger() takes exactly 2 arguments (1 given)")
    }
    
    @Test fun `Passthrough keypress from blocking echo mode`() {
        judo.enterMode(BlockingEchoMode(judo, renderer))
        judo.feedKeys("i")
        assert(judo).isInMode("insert")

        // and make sure the mode stack is as expected
        judo.feedKeys("<esc>")
        assert(judo).isInMode("normal")
    }

    @Test fun `Invalid commands still get added to command mode history`() {
        judo.feedKeys(":echo()<cr>:<up>")
        assert(judo.cmdMode.buffer.toString()).isEqualTo("echo()")
        judo.feedKeys("<esc>")

        judo.feedKeys(":doesNotExist()<cr>:<up>")
        assert(judo.cmdMode.buffer.toString()).isEqualTo("doesNotExist()")
    }

    @Test fun `Blank commands do NOT get added to command mode history`() {
        judo.feedKeys(":echo()<cr>:<up>")
        assert(judo.cmdMode.buffer.toString()).isEqualTo("echo()")
        judo.feedKeys("<esc>")

        judo.feedKeys(":<cr>:<up>")
        assert(judo.cmdMode.buffer.toString()).isEqualTo("echo()")
    }

    @Test fun `readCommandLineInput switches to relevant history`() = assertionsWhileTyping(judo) {
        (judo.currentMode as NormalMode).history.push("Normal")

        yieldKeys(":")
        (judo.currentMode as CmdMode).history.push("Cmd")

        yieldKeys("<ctrl-f>k")
        assert((judo.currentMode as NormalMode).buffer.toString())
            .isEqualTo("Cmd")

        // when we pop back to normal mode, its history should return
        yieldKeys("<ctrl-c><esc>k")
        assert((judo.currentMode as NormalMode).buffer.toString())
            .isEqualTo("Normal")
    }

    @Test fun `Complete empty buffer after submit input`() {
        judo.connection = mock {  }
        judo.feedKeys("ihelp news<cr>")
        judo.feedKeys("h<tab> n<tab><cr>")
        assert(renderer.outputLines).containsExactly(
            "help news",
            "help news"
        )

        judo.feedKeys("<tab>")
        assert(renderer.outputLines).isEqualTo(listOf(
            "help news",
            "help news"
        ))
        assert(judo.buffer.toString()).isNotEmpty()
    }

    @Test fun `esc cancels input() and returns null`() = assertionsWhileTyping(judo) {
        yieldKeys(":print(input('test: '))<cr>hi<esc>")
        assert(renderer.outputLines).isEqualTo(listOf("None"))
    }
}

private fun JudoCore.appendOutput(buffer: String) =
    appendOutput(FlavorableStringBuilder.withDefaultFlavor(buffer))

private fun fakeMode(name: String): Mode =
    object : Mode by Proxy() {
        override val name = name
    }
