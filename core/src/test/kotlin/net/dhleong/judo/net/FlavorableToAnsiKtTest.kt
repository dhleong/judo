package net.dhleong.judo.net

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.flavor.flavor
import net.dhleong.judo.util.ansi
import org.junit.Test

/**
 * @author dhleong
 */
class FlavorableToAnsiKtTest {
    @Test fun `Handle default flavor`() {
        val result = FlavorableStringBuilder.withDefaultFlavor("Take my love").toAnsi()

        assertThat(result).isEqualTo(
            "${ansi(0)}Take my love"
        )
    }

    @Test fun `Changing flags`() {
        val result = FlavorableStringBuilder(64).apply {
            append("Take ", flavor(isBold = true))
            append("my ",
                flavor(
                    isBold = true,
                    isItalic = true
                )
            )
            append("love",
                flavor(isItalic = true)
            )
        }.toAnsi()

        assertThat(result).isEqualTo(
            "${ansi(1)}Take ${ansi(3)}my ${ansi(21)}love${ansi(0)}"
        )
    }
}