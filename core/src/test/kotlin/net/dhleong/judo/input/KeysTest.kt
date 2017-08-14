package net.dhleong.judo.input

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * @author dhleong
 */
class KeysTest {
    @Test fun equality() {
        assertThat(keys("<space>ps"))
            .isEqualTo(keys("<space>ps"))
            .isNotEqualTo(keys("<space>sp"))

        assertThat(keys("<space>ps"))
            .extracting { it.char }
            .containsExactly(' ', 'p', 's')
    }

    @Test fun parseSpecialKeys() {
        // by special I mean we use it to input
        // keys like `<ctrl d>`
        assertThat(keys("<"))
            .extracting { it.char }
            .containsExactly('<')

        assertThat(keys(">"))
            .extracting { it.char }
            .containsExactly('>')
    }

    @Test fun parseSequentialSpecial() {
        assertThat(keys("<<"))
            .extracting { it.char }
            .containsExactly('<', '<')

        assertThat(keys("<>"))
            .extracting { it.char }
            .containsExactly('<', '>')
    }
}