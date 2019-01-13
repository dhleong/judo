package net.dhleong.judo.net

import assertk.assert
import assertk.assertions.isEqualTo
import net.dhleong.judo.render.Flavor
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.SimpleFlavor
import net.dhleong.judo.render.parseAnsi
import net.dhleong.judo.util.ansi
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class AnsiFlavorableStringReaderTest {

    lateinit var reader: AnsiFlavorableStringReader

    @Before fun setUp() {
        reader = AnsiFlavorableStringReader()
    }

    @Test fun `Split lines with single newline`() {
        val all = reader.feed("Take my\nlove\n")
        assert(all.toStringsList()).isEqualTo(listOf(
            "Take my\n",
            "love\n"
        ))
    }

    @Test fun `Split lines with crlf`() {
        val all = reader.feed("Take my\r\nlove\r\n")
        assert(all.toStringsList()).isEqualTo(listOf(
            // normalize to single \n:
            "Take my\n",
            "love\n"
        ))
    }

    @Test fun `Split lines with lfcr`() {
        val all = reader.feed("Take my\n\rlove\n\r")
        assert(all.toStringsList()).isEqualTo(listOf(
            // normalize to single \n:
            "Take my\n",
            "love\n"
        ))
    }

    @Test fun `Normalize linefeed to newline`() {
        val all = reader.feed("Take my love\r")
        assert(all.toStringsList()).isEqualTo(listOf(
            "Take my love\n"
        ))
    }

    @Test fun `Simple ANSI`() {
        // this test justifies using [ansiToFlavorable] to simplify the following tests
        val line = "${ansi(1,6)}Take my ${ansi(1, 2)}love"
            .parseAnsi()
        assert(line).isEqualTo(
            FlavorableStringBuilder(7).apply {
                append("Take my ", SimpleFlavor(
                    isBold = true,
                    hasForeground = true,
                    foreground = JudoColor.Simple.from(6)
                ))
                append("love", SimpleFlavor(
                    isBold = true,
                    hasForeground = true,
                    foreground = JudoColor.Simple.from(2)
                ))
            }
        )
    }

    @Test fun `Combine Split ANSI`() {
        val ansi = ansi(1,2)
        val firstHalf = ansi.slice(0..3)
        val secondHalf = ansi.slice(4..ansi.lastIndex)
        assert("$firstHalf$secondHalf").isEqualTo(ansi.toString())

        val all = reader.feed("${ansi(1,6)}Take my $firstHalf") +
            reader.feed("${secondHalf}love")

        assert(all.toList()).isEqualTo(listOf(
            FlavorableStringBuilder(7).apply {
                append("Take my ", SimpleFlavor(
                    isBold = true,
                    hasForeground = true,
                    foreground = JudoColor.Simple.from(6)
                ))
            },

            FlavorableStringBuilder(4).apply {
                append("love", SimpleFlavor(
                    isBold = true,
                    hasForeground = true,
                    foreground = JudoColor.Simple.from(2)
                ))
            }
        ))
    }

    @Test fun `Trailing ansi`() {
        val all = reader.feed("${ansi(1,6)}Take my ${ansi(1, 2)}") +
            reader.feed("love")

        assert(all.toList()).isEqualTo(listOf(
            "${ansi(1, 6)}Take my ".parseAnsi(),
            "${ansi(1, 2)}love".parseAnsi()
        ))
    }

    @Test fun `Handle 256 colors`() {
        assert(
            "${27.toChar()}[38;5;200;48;5;180m200color".parseAnsi()
        ).isEqualTo(
            FlavorableStringBuilder(7).apply {
                append("200color", SimpleFlavor(
                    hasForeground = true,
                    foreground = JudoColor.High256(200),
                    hasBackground = true,
                    background = JudoColor.High256(180)
                ))
            }
        )
    }

    @Test fun `Handle repeated parsing garbage`() {
        val s = "38;5;00700;" // note the garbage from previously parsed output
        val length = 8 // but we're just the first 8 chars
        assert(
            ansiCharsToFlavor(Flavor.default, s.toCharArray(), length)
        ).isEqualTo(
            SimpleFlavor(
                hasForeground = true,
                foreground = JudoColor.Simple.from(7)
            )
        )
    }

    @Test fun `Read lower-256 colors as Simple`() {
        assert(
            "${27.toChar()}[38;5;15mW${27.toChar()}[38;5;7mw".parseAnsi()
        ).isEqualTo(
            FlavorableStringBuilder(7).apply {
                append("W", SimpleFlavor(
                    hasForeground = true,
                    foreground = JudoColor.Simple(JudoColor.Simple.Color.BRIGHT_WHITE)
                ))
                append("w", SimpleFlavor(
                    hasForeground = true,
                    foreground = JudoColor.Simple(JudoColor.Simple.Color.WHITE)
                ))
            }
        )
    }

    @Test fun `Handle RGB color`() {
        assert(
            "${27.toChar()}[38;2;250;50;20mRGB Color".parseAnsi()
        ).isEqualTo(
            FlavorableStringBuilder(7).apply {
                append("RGB Color", SimpleFlavor(
                    hasForeground = true,
                    foreground = JudoColor.FullRGB(250, 50, 20)
                ))
            }
        )
    }

    @Test fun `Continue styled, empty line across newline`() {
        val lineOne = "${ansi(1,1)}\r\n"
        val lineTwo = "Take my love...\r\n"

        val all = reader.feed(lineOne) +
            reader.feed(lineTwo)

        assert(all.toList()).isEqualTo(listOf(
            "${ansi(1, 1)}\n".parseAnsi(),
            "${ansi(1, 1)}Take my love...\n".parseAnsi()
        ))
    }

    @Test fun `Combine successive styles`() {
        val line = "${ansi(fg = 1)}Take ${ansi(bg = 2)}my"

        assert(line.parseAnsi()).isEqualTo(
            FlavorableStringBuilder(64).apply {
                append("Take ", SimpleFlavor(
                    hasForeground = true,
                    foreground = JudoColor.Simple.from(1)
                ))
                append("my", SimpleFlavor(
                    hasForeground = true,
                    foreground = JudoColor.Simple.from(1),
                    hasBackground = true,
                    background = JudoColor.Simple.from(2)
                ))
            }
        )

    }
}

private fun <T> Sequence<T>.toStringsList() = toList().map { it.toString() }

private fun AnsiFlavorableStringReader.feed(chars: String) =
    feed(chars.toCharArray())

