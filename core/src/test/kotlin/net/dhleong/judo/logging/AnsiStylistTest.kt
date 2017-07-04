package net.dhleong.judo.logging

import net.dhleong.judo.assertThat
import net.dhleong.judo.util.ESCAPE_CHAR
import net.dhleong.judo.util.ansi
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class AnsiStylistTest {
    internal lateinit var stylist: AnsiStylist

    @Before fun setUp() {
        stylist = AnsiStylist()
    }

    @Test fun readBasicColors() {
        stylist.readAnsi(ansi(fg=2))
        assertThat(stylist)
            .hasFg(2)
            .hasNoBg()

        stylist.readAnsi(ansi(bg=3))
        assertThat(stylist)
            .hasFg(2)
            .hasBg(3)
    }

    @Test fun readFgAndBg() {
        stylist.readAnsi(ansi(fg=2, bg=3))
        assertThat(stylist)
            .hasFg(2)
            .hasBg(3)
    }

    @Test fun readSeveralFlags() {
        stylist.readAnsi(ansi(attr=1, fg=2, bg=3))
        assertThat(stylist)
            .isBold()
            .hasFg(2)
            .hasBg(3)
    }

    @Test fun disableFlags() {
        stylist.bold = true

        stylist.readAnsi(ansi(attr=21))
        assertThat(stylist)
            .isNotBold()
    }

    @Test fun read256Color() {
        stylist.readAnsi("$ESCAPE_CHAR[38;5;212m")
        assertThat(stylist).hasFg(212)
    }

    @Test fun readTrueColor() {
        stylist.readAnsi("$ESCAPE_CHAR[38;2;15;14;13m")
        assertThat(stylist).hasFg(0xFE0F0E0D.toInt())

        stylist.readAnsi("$ESCAPE_CHAR[48;2;0;0;0m")
        assertThat(stylist).hasBg(0xFE000000.toInt())
    }
}

private fun AnsiStylist.readAnsi(input: CharSequence) =
    readAnsi(input, 0)
