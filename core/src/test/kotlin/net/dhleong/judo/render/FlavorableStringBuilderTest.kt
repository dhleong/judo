package net.dhleong.judo.render

import assertk.all
import assertk.assertThat
import assertk.assertions.hasLength
import assertk.assertions.hasToString
import assertk.assertions.isSuccess
import net.dhleong.judo.render.flavor.Flavor
import net.dhleong.judo.render.flavor.flavor
import org.junit.Test

class FlavorableStringBuilderTest {
    @Test fun `Basic toString()`() {
        assertThat(FlavorableStringBuilder.fromString("mreynolds"))
            .hasToString("mreynolds")
    }

    @Test fun `Basic append`() {
        val b = FlavorableStringBuilder.fromString("Mal")
        b += " Reynolds"
        assertThat(b).all {
            hasToString("Mal Reynolds")
            hasLength(12)
        }
    }

    @Test fun `append empty FSB`() {
        assertThat {
            val b = FlavorableStringBuilder(1)
            b.append('m', Flavor.default)
            b.append(FlavorableStringBuilder.EMPTY)
        }.isSuccess()
    }

    @Test fun `Append subsequence with flavor`() {
        val b = FlavorableStringBuilder(1).apply {
            append("bla bla mal reynolds", 8, 20,
                flavor(isBold = true)
            )
        }

        assertThat(b).all {
            hasToString("mal reynolds")
            hasFlavor(flavor(isBold = true))
        }
    }

    @Test fun `Expand tabs`() {
        val b = FlavorableStringBuilder(16).apply {
            this += "mal\treynolds"
        }
        assertThat(b).all {
            hasToString("mal  reynolds")
        }
    }

    @Test fun `Continue old flavor if not otherwise specified in append (String)`() {
        val b = FlavorableStringBuilder.fromString("Mal")
        b.beginFlavor(flavor(isBold = true), 0)

        b += " Reynolds"
        assertThat(b).all {
            hasToString("Mal Reynolds")
            hasFlavor(flavor(isBold = true))
            hasLength(12)
        }
    }

    @Test fun `Continue old flavor if not otherwise specified in append (Flavorable)`() {
        val b = FlavorableStringBuilder.fromString("Mal")
        b.beginFlavor(flavor(isBold = true), 0)

        b += FlavorableStringBuilder.fromString(" Reynolds")
        assertThat(b).all {
            hasToString("Mal Reynolds")
            hasFlavor(flavor(isBold = true))
            hasLength(12)
        }
    }

    @Test fun `Keep new flavor in append`() {
        val b = FlavorableStringBuilder.fromString("Mal").apply {
            beginFlavor(flavor(isBold = true), 0)
        }

        b += FlavorableStringBuilder.fromString(" Reynolds").apply {
            beginFlavor(flavor(isItalic = true), 0)
        }
        assertThat(b).all {
            hasToString("Mal Reynolds")
            hasFlavor(flavor(isBold = true), untilIndex = 3)
            hasFlavor(flavor(isItalic = true), atIndex = 3)
            hasLength(12)
        }
    }

    @Test fun `Keep partial new flavor in append`() {
        val b = FlavorableStringBuilder.fromString("Mal").apply {
            beginFlavor(flavor(isBold = true), 0)
        }

        b += FlavorableStringBuilder.fromString(" Reynolds").apply {
            beginFlavor(flavor(isItalic = true), 4)
        }
        assertThat(b).all {
            hasToString("Mal Reynolds")
            hasFlavor(flavor(isBold = true), untilIndex = 7)
            hasFlavor(flavor(isItalic = true), atIndex = 7)
            hasLength(12)
        }
    }

    @Test fun `subSequence works as expected`() {
        val b = FlavorableStringBuilder.fromString("mreynolds")
        val sub = b.subSequence(1, 4)
        assertThat(sub).hasToString("rey")
    }

    @Test fun `append to subSequence works as expected`() {
        val b = FlavorableStringBuilder.fromString("mreynolds")
        val sub = b.subSequence(1, 4)
        sub += "noldo"
        assertThat(sub).hasToString("reynoldo")
    }

    @Test fun `append subSequence to subSequence works as expected`() {
        val b = FlavorableStringBuilder.fromString("mreynolds")
        val sub1 = b.subSequence(1, 4)
        val sub2 = b.subSequence(4, 8)

        sub1 += sub2
        assertThat(sub1).hasToString("reynold")
    }

    @Test fun `subSequence edge cases`() {
        val b = FlavorableStringBuilder.fromString("mreynolds")
        assertThat(b.subSequence(0, 0)).hasToString("")
        assertThat(b.subSequence(0, 9)).hasToString("mreynolds")
        assertThat(b.subSequence(9, 9)).hasToString("")
    }

    @Test fun `splitAtNewlines() handles the empty string`() {
        val b = FlavorableStringBuilder.fromString("")
        assertThat(b).splitsAtNewlinesToStrings(
            ""
        )
    }

    @Test fun `splitAtNewlines() handles only newline`() {
        val b = FlavorableStringBuilder.fromString("\n")
        assertThat(b).splitsAtNewlinesToStrings(
            "\n"
        )
    }

    @Test fun `splitAtNewlines() handles the no-newline case`() {
        val b = FlavorableStringBuilder.fromString("Take my love")
        assertThat(b).splitsAtNewlinesToStrings(
            "Take my love"
        )
    }

    @Test fun `splitAtNewlines() handles the single-newline case`() {
        val b = FlavorableStringBuilder.fromString("Take my love\n")
        assertThat(b).splitsAtNewlinesToStrings(
            "Take my love\n"
        )
    }

    @Test fun `splitAtNewlines() includes the newlines`() {
        val b = FlavorableStringBuilder.fromString("Take\nmy\nlove\n")
        assertThat(b).splitsAtNewlinesToStrings(
            "Take\n",
            "my\n",
            "love\n"
        )
    }

    @Test fun `splitAtNewlines() can continue a single line`() {
        val b = FlavorableStringBuilder.fromString(" love")
        assertThat(b).splitsAtNewlinesToStrings(
            "Take my love",
            continueLines = mutableListOf("Take my")
        )
    }

    @Test fun `splitAtNewlines() can continue multiple lines`() {
        val b = FlavorableStringBuilder.fromString("ke\nmy\nlove\n")
        assertThat(b).splitsAtNewlinesToStrings(
            "Take\n",
            "my\n",
            "love\n",
            continueLines = mutableListOf("Ta")
        )
    }

    @Test fun `replace() at beginning`() {
        val b = FlavorableStringBuilder.fromString("Took my love")
        b.replace(0, 4, "Take")
        assertThat(b).hasToString("Take my love")
    }

    @Test fun `replace() at beginning with more characters`() {
        val b = FlavorableStringBuilder.fromString("Oh my love").apply {
            beginFlavor(flavor(isItalic = true), 2)
            beginFlavor(flavor(isBold = true), 0)
        }

        b.replace(0, 2, "Take")
        assertThat(b).all {
            hasToString("Take my love")

            hasFlavor(flavor(isBold = true), untilIndex = 4)
            hasFlavor(flavor(isItalic = true), atIndex = 4)
        }
    }

    @Test fun `replace() at beginning with fewer characters`() {
        val b = FlavorableStringBuilder.fromString("Take my love").apply {
            beginFlavor(flavor(isItalic = true), 4)
            beginFlavor(flavor(isBold = true), 0)
        }

        b.replace(0, 4, "Oh")
        assertThat(b).all {
            hasToString("Oh my love")

            hasFlavor(flavor(isBold = true), untilIndex = 2)
            hasFlavor(flavor(isItalic = true), atIndex = 2)
        }
    }

    @Test fun `Deep copy empty FSB`() {
        val b = FlavorableStringBuilder(FlavorableStringBuilder.EMPTY)
        assertThat(b).all {
            hasToString("")
            hasLength(0)
        }
    }
}

