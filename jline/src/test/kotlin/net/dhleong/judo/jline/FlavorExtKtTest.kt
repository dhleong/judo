package net.dhleong.judo.jline

import assertk.assert
import assertk.assertions.isEqualTo
import net.dhleong.judo.render.SimpleFlavor
import org.jline.utils.AttributedStyle
import org.junit.Assert.*
import org.junit.Test

class FlavorExtKtTest {
    @Test fun `Single attribute conversion`() {
        assert(SimpleFlavor(isBold = true).toAttributedStyle())
            .isEqualTo(AttributedStyle.BOLD)
    }
}