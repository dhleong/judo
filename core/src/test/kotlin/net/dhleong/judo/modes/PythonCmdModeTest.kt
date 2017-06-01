package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
class PythonCmdModeTest {
    val echos = ArrayList<Any?>()
    val sends = ArrayList<String>()
    val maps = ArrayList<Array<Any>>()

    val mode = PythonCmdMode(object : IJudoCore {
        override fun quit() {
            TODO("not implemented")
        }

        override val aliases
            get() = throw UnsupportedOperationException()

        override fun enterMode(modeName: String) {
            TODO("not implemented")
        }

        override fun feedKey(stroke: KeyStroke, remap: Boolean) {
            TODO("not implemented")
        }

        override fun map(mode: String, from: String, to: String, remap: Boolean) {
            maps.add(arrayOf(mode, from, to, remap))
        }

        override fun send(text: String) {
            sends.add(text)
        }

        override fun echo(vararg objects: Any?) {
            objects.forEach { echos.add(it) }
        }
    })

    @Before fun setUp() {
        mode.onEnter()
        echos.clear()
        sends.clear()
        maps.clear()
    }

    @Test fun echo() {
        mode.execute("echo('test', 2)")

        assertThat(echos).containsExactly("test", 2)
    }

    @Test fun send_ignoreExtraArgs() {
        mode.execute("send('test', 2)")

        assertThat(sends).containsExactly("test")
    }

    @Test fun map_nnoremap() {
        mode.execute("nnoremap('a', 'bc')")

        assertThat(maps)
            .containsExactly(arrayOf("normal", "a", "bc", false))
    }

    @Test fun map_createmap() {
        mode.execute("createmap('custom', 'a', 'bc', True)")

        assertThat(maps)
            .containsExactly(arrayOf("custom", "a", "bc", true))
    }
}

