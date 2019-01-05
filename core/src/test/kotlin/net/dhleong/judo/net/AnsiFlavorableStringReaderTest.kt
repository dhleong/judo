package net.dhleong.judo.net

import assertk.assert
import assertk.assertions.isEqualTo
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.SimpleFlavor
import net.dhleong.judo.util.ansi
import org.junit.Before
import org.junit.Ignore
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

    @Ignore
    @Test fun `Handle RGB and 256 colors`() {
        TODO("also, whatever 90 and 100 mean")
    }

    private fun String.ansiToFlavorable(): FlavorableCharSequence =
        AnsiFlavorableStringReader().feed(this).toList().first()
}

private fun <T> Sequence<T>.toStringsList() = toList().map { it.toString() }

private fun AnsiFlavorableStringReader.feed(chars: String) =
    feed(chars.toCharArray())
