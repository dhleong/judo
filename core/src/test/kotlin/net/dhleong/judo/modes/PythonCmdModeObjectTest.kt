package net.dhleong.judo.modes

import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.TestableJudoRenderer
import net.dhleong.judo.complete.DumbCompletionSource
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.util.InputHistory
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * @author dhleong
 */
class PythonCmdModeObjectTest {

    val judo = TestableJudoCore()
    val input = InputBuffer()
    val mode = PythonCmdMode(
        judo, IdManager(), input,
        TestableJudoRenderer(),
        InputHistory(input),
        DumbCompletionSource()
    )

    @Before fun setUp() {
        mode.onEnter()
        judo.clearTestable()
    }


    @Test fun noAccessToNonExposedFunctions() {
        Assertions.assertThatThrownBy {
            mode.execute("""
            judo.clearTestable()  # public fn on TestableJudoCore, but not in IJudoCore
            """.trimIndent())
        }.hasMessageContaining("no attribute 'clearTestable'")
    }

    @Test fun createMap() {
        val file = File("test.map")
        if (file.exists()) file.delete()
        assertThat(file).doesNotExist()

        mode.execute("""
            judo.mapper.createEmpty()
            judo.mapper.saveAs("${file.absolutePath}")
            """.trimIndent())
        assertThat(file).exists()

        mode.execute("""
            echo(judo.mapper.current is not None)
            """.trimIndent())

        assertThat(judo.echos).containsExactly(true)

        file.delete()
    }

}