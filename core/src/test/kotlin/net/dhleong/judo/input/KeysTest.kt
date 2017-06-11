package net.dhleong.judo.input

import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

/**
 * @author dhleong
 */
class KeysTest {
    @Test fun parseSpecialKeys() {
        // by special I mean we use it to input
        // keys like `<ctrl d>`
        assertThat(keys("<"))
            .extracting { it.keyChar }
            .containsExactly('<')

        assertThat(keys(">"))
            .extracting { it.keyChar }
            .containsExactly('>')
    }

    @Ignore("TODO: keys(\"<<\")")
    @Test fun parseSequentialSpecial() {
        assertThat(keys("<<"))
            .extracting { it.keyChar }
            .containsExactly('<', '<')

        assertThat(keys("<>"))
            .extracting { it.keyChar }
            .containsExactly('<', '>')
    }
}