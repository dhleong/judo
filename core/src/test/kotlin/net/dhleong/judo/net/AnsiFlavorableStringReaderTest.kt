package net.dhleong.judo.net

import assertk.assert
import assertk.assertions.isEqualTo
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.SimpleFlavor
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
            .ansiToFlavorable()
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
            "${ansi(1, 6)}Take my ".ansiToFlavorable(),
            "${ansi(1, 2)}love".ansiToFlavorable()
        ))
    }

    @Test fun `Handle 256 colors`() {
        assert(
            "${27.toChar()}[38;5;200;48;5;180m200color".ansiToFlavorable()
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

    @Test fun `Handle RGB color`() {
        assert(
            "${27.toChar()}[38;2;250;50;20mRGB Color".ansiToFlavorable()
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
            "${ansi(1, 1)}\n".ansiToFlavorable(),
            "${ansi(1, 1)}Take my love...\n".ansiToFlavorable()
        ))
    }
}

private fun <T> Sequence<T>.toStringsList() = toList().map { it.toString() }

private fun AnsiFlavorableStringReader.feed(chars: String) =
    feed(chars.toCharArray())

fun String.ansiToFlavorable(): FlavorableCharSequence =
    AnsiFlavorableStringReader().feed(this).toList().first()
