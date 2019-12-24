package net.dhleong.judo.jline

import assertk.all
import assertk.assert
import assertk.assertThat
import assertk.assertions.hasLength
import assertk.assertions.hasToString
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.SimpleFlavor
import org.jline.utils.AttributedStyle
import org.junit.Test

/**
 * @author dhleong
 */
class FlavorableSequenceExtKtTest {
    @Test fun `Flavorable to JLine string`() {
        val flavorable = FlavorableStringBuilder(64).apply {
            append("Take ", SimpleFlavor(isBold = true))
            append("My ", SimpleFlavor(isItalic = true))
            append("Love", SimpleFlavor(isUnderline = true))
        }
        val jline = flavorable.toAttributedString()
        assertThat(jline).all {
            hasToString("Take My Love")
            hasStyleAt(0, AttributedStyle.BOLD)
        }
    }

    @Test(timeout = 2000) fun `Convert empty sequence with trailing flavor to JLine`() {
        val flavorable = FlavorableStringBuilder(16).apply {
            trailingFlavor = SimpleFlavor(isBold = true)
        }
        val attr = flavorable.toAttributedString(10)
        assertThat(attr).all {
            hasLength(1)
            hasStyleAt(0, AttributedStyle.BOLD)
        }
    }
}

