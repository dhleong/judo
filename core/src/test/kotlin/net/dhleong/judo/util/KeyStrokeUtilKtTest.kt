package net.dhleong.judo.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
class KeyStrokeUtilKtTest {
    @Test fun describeBasic() {
        assertThat(key("i").describe()).isEqualTo("i")
        assertThat(key("I").describe()).isEqualTo("I")
    }

    @Test fun describeBasicWithModifiers() {
        assertThat(key("alt ctrl i").describe()).isEqualTo("<alt ctrl i>")
        assertThat(key("ctrl i").describe()).isEqualTo("<ctrl i>")
        assertThat(key("alt I").describe()).isEqualTo("<alt I>")
    }

    @Test fun describeSpecial() {
        assertThat(key(" ").describe()).isEqualTo("<space>")
        assertThat(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0).describe()).isEqualTo("<esc>")
    }

    @Test fun describeSpecialWithModifiers() {
        assertThat(key("alt bs").describe()).isEqualTo("<alt bs>")

        assertThat(KeyStroke.getKeyStroke(
            KeyEvent.VK_SPACE,
            KeyEvent.SHIFT_DOWN_MASK
        ).describe()).isEqualTo("<shift space>")

        assertThat(KeyStroke.getKeyStroke(
            KeyEvent.VK_ESCAPE, KeyEvent.CTRL_DOWN_MASK
        ).describe()).isEqualTo("<ctrl esc>")

        assertThat(KeyStroke.getKeyStroke(
            KeyEvent.VK_DOWN,
            KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK
        ).describe()).isEqualTo("<ctrl shift down>")

    }

    @Test fun hasCtrl() {
        assertThat(key("i").hasCtrl()).isFalse()
        assertThat(key("ctrl i").hasCtrl()).isTrue()
    }
}