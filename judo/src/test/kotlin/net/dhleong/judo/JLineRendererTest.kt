package net.dhleong.judo

import net.dhleong.judo.jline.JLineRenderer2
import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage2
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.render.OutputLine
import net.dhleong.judo.render.PrimaryJudoWindow2
import net.dhleong.judo.util.stripAnsi
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * TODO: A lot of these are more like JudoCore+JudoWindow/JudoBuffer integration tests
 * But since JudoCore depends on a JudoRenderer, we've put them here for now....
 * @author dhleong
 */
class JLineRendererTest {

    lateinit var renderer: JLineRenderer2
    lateinit var mapRenderer: MapRenderer
    lateinit var outputBuffer: JudoBuffer
    lateinit var window: PrimaryJudoWindow2
    lateinit var page: IJudoTabpage2

    val settings = StateMap(
        WORD_WRAP to false
    )

    @Before fun setUp() {
        val ids = IdManager()
        outputBuffer = JudoBuffer(ids)

        renderer = JLineRenderer2(ids, settings, false)
        renderer.windowWidth = 20
        renderer.windowHeight = 6
        renderer.currentTabpage = page
//        renderer.updateSize()
        renderer.setLoading(false)

        page = renderer.currentTabpage
        window = page.currentWindow as PrimaryJudoWindow2

//        mapRenderer = DelegateJLineMapRenderer(renderer)
    }

    @After fun tearDown() {
        renderer.close()
    }

    @Ignore
    @Test fun catchSplitPrompts() {
//        val judo = newJudoCore()
//        judo.prompts.define("HP: $1", "(hp: $1)")
//
//        val prompt = "${ansi(1,3)}HP: ${ansi(1,6)}42\r\n"
//        val first = prompt.substring(0..8)
//        val second = prompt.substring(9..prompt.lastIndex)
//        assertThat("$first$second").isEqualTo(prompt)
//
//        judo.onIncomingBuffer(first.toCharArray(), first.length)
//        judo.onIncomingBuffer(second.toCharArray(), second.length)
//
//        val buffer = judo.renderer.currentTabpage!!.currentWindow.currentBuffer
//        assertThat(buffer.getAnsiContents())
//            .containsExactly("")
    }

    @Ignore
    @Test fun catchSplitPrompts_splitAnsi() {
//
//        val judo = newJudoCore()
//        judo.prompts.define("HP: $1", "(hp: $1)")
//
//        val prompt = "${ansi(1,3)}HP: ${ansi(1,6)}42\r\n"
//        val first = prompt.substring(0..5)
//        val second = prompt.substring(6..prompt.lastIndex)
//        assertThat("$first$second").isEqualTo(prompt)
//
//        judo.onIncomingBuffer(first.toCharArray(), first.length)
//        judo.onIncomingBuffer(second.toCharArray(), second.length)
//
//        val buffer = judo.renderer.currentTabpage!!.currentWindow.currentBuffer
//        assertThat(buffer.getAnsiContents())
//            .containsExactly("")
    }


    @Ignore
    @Test fun continueTrailingStyleAcrossSplitLines() {
//        renderer.windowWidth = 12
//        renderer.windowHeight = 7
//        renderer.updateSize()
//
//        val lineOne = "\r\n${ansi(1,2)}\r\nTake my love\r\nTake"
//        val lineTwo = " my land...\r\nTake me where..."
//
//        assertThat(OutputLine(lineOne).getFinalStyle().toString())
//            .isEqualTo(ansi(1,2).toString())
//
//        val core = newJudoCore()
//        // NOTE: input line takes 1 from the 7 above
//        assertThat(renderer.currentTabpage!!.currentWindow.height).isEqualTo(6)
//
//        core.onIncomingBuffer(lineOne.toCharArray(), lineOne.length)
//        core.onIncomingBuffer(lineTwo.toCharArray(), lineTwo.length)
//
//        val window = core.renderer.currentTabpage!!.currentWindow
//        val buffer = window.currentBuffer
//        if (buffer[0].startsWith("ERROR")) {
//            throw AssertionError(buffer.getAnsiContents().joinToString("\n"))
//        }
//
//        val bufferContents = buffer.getAnsiContents()
//        assertThat(bufferContents[2])
//            .isEqualTo("${ansi(1,2)}Take my love${ansi(0)}")
//        assertThat(bufferContents[3])
//            .isEqualTo("${ansi(1,2)}Take my land${ansi(0)}")
//        assertThat(bufferContents[4])
//            .isEqualTo("${ansi(1,2)}...${ansi(0)}")
//        assertThat(bufferContents[5])
//            .isEqualTo("${ansi(1,2)}Take me where...${ansi(0)}")
//
//        // NOTE: status line takes 1 from the 6 above
//        val outputWindow = (window as PrimaryJudoWindow).outputWindow
//        assertThat(outputWindow.height).isEqualTo(5)
//
//        val windowContents = outputWindow.getDisplayStrings()
//        assertThat(windowContents).hasSize(outputWindow.height)
//        assertThat(windowContents[0])
//            .isEqualTo("${ansi(1,2)}Take my love${ansi(0)}")
//        assertThat(windowContents[1])
//            .isEqualTo("${ansi(1,2)}Take my land${ansi(0)}")
//        assertThat(windowContents[2])
//            .isEqualTo("${ansi(1,2)}...${ansi(0)}")
//        assertThat(windowContents[3])
//            .isEqualTo("${ansi(1,2)}Take me wher${ansi(0)}")
    }
}

fun IJudoBuffer.getRawContents(): List<String> =
    (0..this.lastIndex).map { (this[it] as OutputLine).toAnsi() }

fun IJudoBuffer.getStringContents(): List<String> =
    (0..this.lastIndex).map { stripAnsi(this[it]) }

