package net.dhleong.judo.modes

import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.util.InputHistory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.InputStream

/**
 * @author dhleong
 */
class BaseCmdModeTest {

    val judo = TestableJudoCore()
    val input = InputBuffer()
    val renderer = TestableJudoRenderer()

    lateinit var mode: BaseCmdMode

    @Before fun setUp() {

        mode = object : BaseCmdMode(
            judo, input,
            renderer,
            InputHistory(input)
        ) {
            override fun execute(code: String) {
                TODO("not implemented")
            }

            override fun readFile(fileName: String, stream: InputStream) {
                TODO("not implemented")
            }
        }
    }

    @Test fun helpColumns() {
        val colWidth = "createUserMode  ".length
        renderer.settableWindowWidth = colWidth * 2

        mode.showHelp()

        assertThat(judo.echos)
            .startsWith("alias           cmap            ")

    }
}