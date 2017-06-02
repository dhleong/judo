package net.dhleong.judo.modes

import net.dhleong.judo.TestableJudoCore
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class PythonCmdModeTest {

    val judo = TestableJudoCore()
    val mode = PythonCmdMode(judo)

    @Before fun setUp() {
        mode.onEnter()
        judo.clearTestable()
    }

    @Test fun echo() {
        mode.execute("echo('test', 2)")

        assertThat(judo.echos).containsExactly("test", 2)
    }

    @Test fun send_ignoreExtraArgs() {
        mode.execute("send('test', 2)")

        assertThat(judo.sends).containsExactly("test")
    }

    @Test fun map_nnoremap() {
        mode.execute("nnoremap('a', 'bc')")

        assertThat(judo.maps)
            .containsExactly(arrayOf("normal", "a", "bc", false))
    }

    @Test fun map_createmap() {
        mode.execute("createmap('custom', 'a', 'bc', True)")

        assertThat(judo.maps)
            .containsExactly(arrayOf("custom", "a", "bc", true))
    }
}

