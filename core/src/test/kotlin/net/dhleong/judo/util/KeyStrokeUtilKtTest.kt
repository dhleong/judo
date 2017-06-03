package net.dhleong.judo.util

import net.dhleong.judo.input.key
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * @author dhleong
 */
class KeyStrokeUtilKtTest {
    @Test fun hasCtrl() {
        assertThat(key("i").hasCtrl()).isFalse()
        assertThat(key("ctrl i").hasCtrl()).isTrue()
    }

}