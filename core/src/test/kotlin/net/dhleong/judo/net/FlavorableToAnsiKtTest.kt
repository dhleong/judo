package net.dhleong.judo.net

import assertk.assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.SimpleFlavor
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
            append("Take ", SimpleFlavor(isBold = true))
            append("my ", SimpleFlavor(isBold = true, isItalic = true))
            append("love", SimpleFlavor(isItalic = true))
        }.toAnsi()

        assertThat(result).isEqualTo(
            "${ansi(1)}Take ${ansi(3)}my ${ansi(21)}love${ansi(0)}"
        )
    }
}