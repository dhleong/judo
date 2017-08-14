package net.dhleong.judo.input

import org.junit.Test

/**
 * @author dhleong
 */
class KeyTest {

    @Test fun parseSimple() {
        assertThat(Key.parse("a")).hasChar('a')
        assertThat(Key.parse(";")).hasChar(';')
        assertThat(Key.parse("~")).hasChar('~')
    }

    @Test fun parseString() {
        assertThat(Key.parse("esc")).hasKeyCode(Key.CODE_ESCAPE)
    }

    @Test fun parseUpper() {
        assertThat(Key.parse("A"))
            .hasChar('a')
            .hasModifiers(Modifier.SHIFT)
    }

    @Test fun parseSingleModifier() {
        assertThat(Key.parse("shift a"))
            .hasChar('a')
            .hasModifiers(Modifier.SHIFT)
        assertThat(Key.parse("ctrl a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL)
        assertThat(Key.parse("ctrl-a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL)
    }

    @Test fun parseMultipleModifiers() {
        assertThat(Key.parse("ctrl shift a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL, Modifier.SHIFT)
        assertThat(Key.parse("ctrl-alt-a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL, Modifier.ALT)
        assertThat(Key.parse("ctrl shift tab"))
            .hasKeyCode(Key.CODE_TAB)
            .hasModifiers(Modifier.CTRL, Modifier.SHIFT)
    }

    @Test fun abbreviatedModifiers() {
        assertThat(Key.parse("c s a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL, Modifier.SHIFT)
        assertThat(Key.parse("c-a-a"))
            .hasChar('a')
            .hasModifiers(Modifier.CTRL, Modifier.ALT)
        assertThat(Key.parse("c s tab"))
            .hasKeyCode(Key.CODE_TAB)
            .hasModifiers(Modifier.CTRL, Modifier.SHIFT)
    }

}