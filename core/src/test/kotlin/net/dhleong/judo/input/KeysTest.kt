package net.dhleong.judo.input

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import org.junit.Test

/**
 * @author dhleong
 */
class KeysTest {
    @Test fun equality() {
        assertThat(keys("<space>ps")).all {
            isEqualTo(keys("<space>ps"))
            isNotEqualTo(keys("<space>sp"))
        }

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

        assertThat(keys("\\"))
            .extracting { it.char }
            .containsExactly('\\')

        assertThat(keys("<ctrl-BACKSLASH>"))
            .extracting { it.char }
            .contains('\\')

        assertThat(keys("<ctrl-BACKSLASH><ctrl-n>"))
            .extracting { it.char }
            .contains('\\')
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