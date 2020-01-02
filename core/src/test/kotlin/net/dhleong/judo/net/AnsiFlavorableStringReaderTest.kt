package net.dhleong.judo.net

import assertk.all
import assertk.assertThat
import assertk.assertions.hasLength
import assertk.assertions.hasSize
import assertk.assertions.hasToString
import assertk.assertions.isEqualTo
import net.dhleong.judo.prompt.PromptManager
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.doesNotHaveTrailingFlavor
import net.dhleong.judo.render.flavor.Flavor
import net.dhleong.judo.render.flavor.flavor
import net.dhleong.judo.render.hasFlavor
import net.dhleong.judo.render.hasTrailingFlavor
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
        assertThat(all.toStringsList()).isEqualTo(listOf(
            "Take my\n",
            "love\n"
        ))
    }

    @Test fun `Split lines with crlf`() {
        val all = reader.feed("Take my\r\nlove\r\n")
        assertThat(all.toStringsList()).isEqualTo(listOf(
            // normalize to single \n:
            "Take my\n",
            "love\n"
        ))
    }

    @Test fun `Split lines with lfcr`() {
        val all = reader.feed("Take my\n\rlove\n\r")
        assertThat(all.toStringsList()).isEqualTo(listOf(
            // normalize to single \n:
            "Take my\n",
            "love\n"
        ))
    }

    @Test fun `Normalize linefeed to newline`() {
        val all = reader.feed("Take my love\r")
        assertThat(all.toStringsList()).isEqualTo(listOf(
            "Take my love\n"
        ))
    }

    @Test fun `Simple ANSI`() {
        // this test justifies using [ansiToFlavorable] to simplify the following tests
        val line = "${ansi(1,6)}Take my ${ansi(1, 2)}love"
            .parseAnsi()
        assertThat(line).isEqualTo(
            FlavorableStringBuilder(7).apply {
                append("Take my ", flavor(
                    isBold = true,
                    foreground = JudoColor.Simple.from(6)
                ))
                append("love", flavor(
                    isBold = true,
                    foreground = JudoColor.Simple.from(2)
                ))
            }
        )
    }

    @Test fun `Combine Split ANSI`() {
        val ansi = ansi(1,2)
        val firstHalf = ansi.slice(0..3)
        val secondHalf = ansi.slice(4..ansi.lastIndex)
        assertThat("$firstHalf$secondHalf").isEqualTo(ansi.toString())

        val all = reader.feed("${ansi(1,6)}Take my $firstHalf") +
            reader.feed("${secondHalf}love")

        assertThat(all.toList()).isEqualTo(listOf(
            FlavorableStringBuilder(7).apply {
                append("Take my ", flavor(
                    isBold = true,
                    foreground = JudoColor.Simple.from(6)
                ))
            },

            FlavorableStringBuilder(4).apply {
                append("love", flavor(
                    isBold = true,
                    foreground = JudoColor.Simple.from(2)
                ))
            }
        ))
    }

    @Test fun `Continue trailing ansi`() {
        val all = reader.feed("${ansi(1,6)}Take my ${ansi(1, 2)}") +
            reader.feed("love")

        assertThat(all.toList()).isEqualTo(listOf(
            "${ansi(1, 6)}Take my ".parseAnsi(),
            "${ansi(1, 2)}love".parseAnsi()
        ))
    }

    @Test fun `Store trailing ansi`() {
        val all = (
            reader.feed("${ansi(1,6)}Take my ${ansi(1, 2)}\n") +
            reader.feed("love")
        ).toList()

        val trailingFlavor = flavor(
            isBold = true,
            foreground = JudoColor.Simple.from(2)
        )

        assertThat(all).hasSize(2)
        assertThat(all[0]).all {
            hasFlavor(trailingFlavor, atIndex = 10)
            hasTrailingFlavor(trailingFlavor)
            hasToString("Take my \n")
        }

        assertThat(all[1]).all {
            doesNotHaveTrailingFlavor()
            hasFlavor(trailingFlavor)
            hasToString("love")
        }
    }

    @Test fun `Trailing ANSI storage 2`() {
        val raw = "\u001B[48;5;234m  \u001B[0;38;5;007;48;5;000m\r\u001B[38;5;000;48;5;232m"
        assertThat(raw.parseAnsi()).all {
            hasLength(4)
            hasTrailingFlavor(
                flavor(
                    foreground = JudoColor.Simple.from(7),
                    background = JudoColor.Simple.from(0)
                )
            )
        }
    }

    @Test fun `Process sequence with Trailing ANSI`() {
        val original = "\u001B[48;5;234m  \u001B[0;38;5;007;48;5;000m\r\u001B[38;5;000;48;5;232m".parseAnsi()
        val processed = PromptManager()
            .process(original) { _, _, _ ->
                /* nop */
            }
        assertThat(processed).all {
            hasLength(4)
            hasTrailingFlavor(
                flavor(
                    foreground = JudoColor.Simple.from(7),
                    background = JudoColor.Simple.from(0)
                )
            )
        }
    }

    @Test fun `Handle 256 colors`() {
        assertThat(
            "${27.toChar()}[38;5;200;48;5;180m200color".parseAnsi()
        ).isEqualTo(
            FlavorableStringBuilder(7).apply {
                append("200color", flavor(
                    foreground = JudoColor.High256(200),
                    background = JudoColor.High256(180)
                ))
            }
        )
    }

    @Test fun `Handle repeated parsing garbage`() {
        val s = "38;5;00700;" // note the garbage from previously parsed output
        val length = 8 // but we're just the first 8 chars
        assertThat(
            ansiCharsToFlavor(Flavor.default, s.toCharArray(), length)
        ).isEqualTo(
            flavor(foreground = JudoColor.Simple.from(7))
        )
    }

    @Test fun `Read lower-256 colors as Simple`() {
        assertThat(
            "${27.toChar()}[38;5;15mW${27.toChar()}[38;5;7mw".parseAnsi()
        ).isEqualTo(
            FlavorableStringBuilder(7).apply {
                append("W", flavor(
                    foreground = JudoColor.Simple(JudoColor.Simple.Color.BRIGHT_WHITE)
                ))
                append("w", flavor(
                    foreground = JudoColor.Simple(JudoColor.Simple.Color.WHITE)
                ))
            }
        )
    }

    @Test fun `Handle RGB color`() {
        assertThat(
            "${27.toChar()}[38;2;250;50;20mRGB Color".parseAnsi()
        ).isEqualTo(
            FlavorableStringBuilder(9).apply {
                append("RGB Color", flavor(
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

        assertThat(all.toList()).isEqualTo(listOf(
            "${ansi(1, 1)}\n".parseAnsi(),
            "${ansi(1, 1)}Take my love...\n".parseAnsi()
        ))
    }

    @Test fun `Combine successive styles`() {
        val line = "${ansi(fg = 1)}Take ${ansi(bg = 2)}my"

        assertThat(line.parseAnsi()).isEqualTo(
            FlavorableStringBuilder(64).apply {
                append("Take ", flavor(
                    foreground = JudoColor.Simple.from(1)
                ))
                append("my", flavor(
                    foreground = JudoColor.Simple.from(1),
                    background = JudoColor.Simple.from(2)
                ))
            }
        )

    }
}

private fun <T> Sequence<T>.toStringsList() = toList().map { it.toString() }

private fun AnsiFlavorableStringReader.feed(chars: String) =
    feed(chars.toCharArray())

