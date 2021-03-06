package net.dhleong.judo

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.each
import assertk.assertions.exists
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.none
import assertk.assertions.text
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.timeout
import kotlinx.coroutines.runBlocking
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.modes.BlockingEchoMode
import net.dhleong.judo.modes.CmdMode
import net.dhleong.judo.modes.NormalMode
import net.dhleong.judo.net.AnsiFlavorableStringReader
import net.dhleong.judo.net.toAnsi
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.PrimaryJudoWindow
import net.dhleong.judo.render.flavor.flavor
import net.dhleong.judo.render.toFlavorable
import net.dhleong.judo.util.ansi
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
            connections = DummyConnectionFactory,
            debug = DebugLevel.NORMAL
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
        assertThat(renderer.outputLines).containsExactly(
            "Mal Reynolds"
        )
        assertThat(renderer.flavoredOutputLines).containsExactly(
            FlavorableStringBuilder(16).apply {
                append("Mal ",
                    flavor(isBold = true)
                )
                append("Reynolds",
                    flavor(
                        isBold = true,
                        isItalic = true
                    )
                )
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
        assertThat("$first$second").isEqualTo(prompt)

        val reader = AnsiFlavorableStringReader()
        val sequences = reader.feed(first.toCharArray()) +
            reader.feed(second.toCharArray())

        for (s in sequences) {
            judo.onIncomingBuffer(s)
        }

        val buffer = judo.renderer.currentTabpage.currentWindow.currentBuffer
        assertThat(buffer).hasSize(0)
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
        assertThat(promptBuffer).hasLines(
            "<42>\n"
        )

        judo.onIncomingBuffer("HP[42]>\n".toFlavorable())
        assertThat(promptBuffer).hasLines(
            "HP: 42\n"
        )

        judo.onIncomingBuffer("Ammo: 22>\n".toFlavorable())
        assertThat(promptBuffer).hasLines(
            "HP: 42\n",
            "Ammo: 22\n"
        )

        // back to group -1
        judo.onIncomingBuffer("HP 32>\n".toFlavorable())
        assertThat(promptBuffer).hasLines(
            "<32>\n"
        )
    }

    @Test fun feedKeys() = runBlocking {
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

        assertThat(tmpFile).all {
            exists()
            text().transform { it.trim() }.isEqualTo("take love\ntake land")
        }

        tmpFile.deleteOnExit()
    }

    @Test fun resetCompletionOnModeChange() = runBlocking {
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

    @Test fun triggerScriptingError() = runBlocking {
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
    
    @Test fun `Passthrough keypress from blocking echo mode`() = runBlocking {
        judo.enterMode(BlockingEchoMode(judo, renderer))
        judo.feedKeys("i")
        assertThat(judo).isInMode("insert")

        // and make sure the mode stack is as expected
        judo.feedKeys("<esc>")
        assertThat(judo).isInMode("normal")
    }

    @Test fun `Invalid commands still get added to command mode history`() = runBlocking {
        judo.feedKeys(":echo()<cr>:<up>")
        assertThat(judo.cmdMode.buffer.toString()).isEqualTo("echo()")
        judo.feedKeys("<esc>")

        judo.feedKeys(":doesNotExist()<cr>:<up>")
        assertThat(judo.cmdMode.buffer.toString()).isEqualTo("doesNotExist()")
    }

    @Test fun `Blank commands do NOT get added to command mode history`() = runBlocking {
        judo.feedKeys(":echo()<cr>:<up>")
        assertThat(judo.cmdMode.buffer.toString()).isEqualTo("echo()")
        judo.feedKeys("<esc>")

        judo.feedKeys(":<cr>:<up>")
        assertThat(judo.cmdMode.buffer.toString()).isEqualTo("echo()")
    }

    @Test fun `readCommandLineInput switches to relevant history`() = assertionsWhileTyping(judo) {
        (judo.currentMode as NormalMode).history.push("Normal")

        yieldKeys(":")
        (judo.currentMode as CmdMode).history.push("Cmd")

        yieldKeys("<ctrl-f>k")
        assertThat((judo.currentMode as NormalMode).buffer.toString())
            .isEqualTo("Cmd")

        // when we pop back to normal mode, its history should return
        yieldKeys("<ctrl-c><esc>k")
        assertThat((judo.currentMode as NormalMode).buffer.toString())
            .isEqualTo("Normal")
    }

    @Test fun `Complete empty buffer after submit input`() = assertionsWhileTyping(judo) {
        judo.connection = mock {  }
        yieldKeys("ihelp news<cr>")
        yieldKeys("h<tab> n<tab><cr>")
        assertThat(renderer.outputLines).containsExactly(
            "help news",
            "help news"
        )

        yieldKeys("<tab>")
        assertThat(renderer.outputLines).isEqualTo(listOf(
            "help news",
            "help news"
        ))
        assertThat(judo.buffer.toString()).isNotEmpty()
    }

    @Test(timeout = 5000) fun `Don't cause recursion from triggers that send`() = runBlocking {
        judo.connection = mock {  }
        judo.triggers.define("You can't take") {
            judo.send("the skies", fromMap = true)
        }
        judo.appendOutput("You can't take them\n")
        assertThat(renderer.outputLines).each {
            it.doesNotContain("recursion")
        }
    }

    @Test(timeout = 10_000) fun `esc cancels input() and returns null`() = assertionsWhileTyping(judo) {
        yieldKeys(":print(input('test: '))<cr>hi<esc>")
        assertThat(renderer.outputLines).isEqualTo(listOf("None"))
    }

    @Test(timeout = 10_000) fun `feed keys via normal() via map`() = runBlocking {
        judo.cmdMode.execute("""
            nnoremap("<space>p", lambda: normal(':print("mreynolds")<cr>'))
        """.trimIndent())

        judo.feedKeys("<space>p")
        assertThat(renderer.outputLines).containsExactly("mreynolds")
    }
}

private fun JudoCore.appendOutput(buffer: String) =
    appendOutput(FlavorableStringBuilder.withDefaultFlavor(buffer))

private fun fakeMode(name: String): Mode =
    object : Mode by Proxy() {
        override val name = name
    }
