package net.dhleong.judo.input

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.awt.event.KeyEvent.VK_BACK_SPACE
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
class InputBufferTest {

    val buffer = InputBuffer()

    @Before fun setUp() {
        buffer.clear()
    }

    @Test fun simpleType() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type(key("'"))
        buffer.type(key("\""))
        buffer.type(key("t"))

        assertThat(buffer.toString()).isEqualTo("'\"t")
        assertThat(buffer.cursor).isEqualTo(3)
    }

    @Test fun backspace() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type("malcolm reynolds")

        assertThat(buffer.toString()).isEqualTo("malcolm reynolds")
        assertThat(buffer.cursor).isEqualTo(16)

        buffer.type(KeyStroke.getKeyStroke(VK_BACK_SPACE, 0))

        assertThat(buffer.toString()).isEqualTo("malcolm reynold")
        assertThat(buffer.cursor).isEqualTo(15)
    }

    @Test fun backspace_middle() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type("malcolm reynolds")
        buffer.cursor = 8
        buffer.type(KeyStroke.getKeyStroke(VK_BACK_SPACE, 0))

        assertThat(buffer.toString()).isEqualTo("malcolmreynolds")
        assertThat(buffer.cursor).isEqualTo(7)
    }

    @Test fun backspace_empty() {
        assertThat(buffer.toString()).isEmpty()

        buffer.type(KeyStroke.getKeyStroke(VK_BACK_SPACE, 0))

        assertThat(buffer.toString()).isEmpty()
        assertThat(buffer.cursor).isEqualTo(0)
    }
}

