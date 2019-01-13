package net.dhleong.judo.jline

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.SimpleFlavor
import net.dhleong.judo.render.parseAnsi
import org.jline.terminal.Terminal
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.jline.utils.InfoCmp
import org.junit.Test

class FlavorExtKtTest {
    @Test fun `Single attribute conversion`() {
        assert(SimpleFlavor(isBold = true).toAttributedStyle())
            .isEqualTo(AttributedStyle.BOLD)
    }

    @Test fun `RGB color conversion`() {
        val originalFlavor = SimpleFlavor(foreground = JudoColor.FullRGB(
            red = 255,
            green = 100,
            blue = 0
        ), hasForeground = true)
        val style = originalFlavor.toAttributedStyle()

        val ansi = AttributedStringBuilder()
            .append("text", style)
            .toAnsi(terminalWith256Colors())

        val flavorable = ansi.parseAnsi()
        val parsedForeground = flavorable.getFlavor(0).foreground
        assert(parsedForeground).isInstanceOf(JudoColor.High256::class.java)
    }

    private fun terminalWith256Colors(): Terminal = mock {
        on { getNumericCapability(eq(InfoCmp.Capability.max_colors)) } doReturn 256
    }
}