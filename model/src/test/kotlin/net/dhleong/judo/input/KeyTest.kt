package net.dhleong.judo.input

import net.dhleong.judo.input.assertThat
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * @author dhleong
 */
class KeyTest {

    @Test fun parseSimple() {
        assertThat(key("a")).hasChar('a')
        assertThat(key(";")).hasChar(';')
        assertThat(key("~")).hasChar('~')
    }

    @Test fun parseString() {
        assertThat(key("esc")).hasKeyCode(Key.CODE_ESCAPE)
    }

    @Test fun parseUpper() {
        assertThat(key("A"))
            .hasChar('A')
    }

    @Test fun parseSingleModifier() {
        assertThat(key("shift a"))
            .hasChar('a')
            .hasModifiers(Modifier.SHIFT)
        assertThat(key("ctrl a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL)
        assertThat(key("ctrl-a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL)
    }

    @Test fun parseMultipleModifiers() {
        assertThat(key("ctrl shift a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL, Modifier.SHIFT)
        assertThat(key("ctrl-alt-a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL, Modifier.ALT)
        assertThat(key("ctrl shift tab"))
            .hasKeyCode(Key.CODE_TAB)
            .hasModifiers(Modifier.CTRL, Modifier.SHIFT)
    }

    @Test fun abbreviatedModifiers() {
        assertThat(key("c s a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL, Modifier.SHIFT)
        assertThat(key("c-a-a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL, Modifier.ALT)
        assertThat(key("c s tab"))
            .hasKeyCode(Key.CODE_TAB)
            .hasModifiers(Modifier.CTRL, Modifier.SHIFT)
    }

    @Test fun describeBasic() {
        assertThat(key("i").describe()).isEqualTo("i")
        assertThat(key("I").describe()).isEqualTo("I")
    }

    @Test fun describeBasicWithModifiers() {
        assertThat(key("alt ctrl i").describe()).isEqualTo("<alt ctrl i>")
        assertThat(key("ctrl i").describe()).isEqualTo("<ctrl i>")

        // NOTE: when modifiers are explicitly provided, it becomes case-insensitive
        assertThat(key("alt I").describe()).isEqualTo("<alt i>")
    }

    @Test fun describeSpecial() {
        assertThat(key(" ").describe()).isEqualTo("<space>")
        assertThat(Key.ESCAPE.describe()).isEqualTo("<esc>")
    }

    @Test fun describeSpecialWithModifiers() {
        assertThat(key("alt bs").describe()).isEqualTo("<alt bs>")

        assertThat(Key.ofChar(' ', Modifier.SHIFT).describe())
            .isEqualTo("<shift space>")

        assertThat(Key.ofChar(
            Key.CODE_ESCAPE.toChar(), Modifier.CTRL
        ).describe()).isEqualTo("<ctrl esc>")

        assertThat(key("c-s-down").describe())
            .isEqualTo("<ctrl shift down>")
    }

    @Test fun hasCtrl() {
        assertThat(key("i").hasCtrl()).isFalse()
        assertThat(key("ctrl i").hasCtrl()).isTrue()
    }

}