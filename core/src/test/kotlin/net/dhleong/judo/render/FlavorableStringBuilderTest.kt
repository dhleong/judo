package net.dhleong.judo.render

import assertk.Assert
import assertk.all
import assertk.assert
import assertk.assertions.hasLength
import assertk.assertions.isEqualTo
import org.junit.Test

class FlavorableStringBuilderTest {
    @Test fun `Basic toString()`() {
        assert(FlavorableStringBuilder.fromString("mreynolds"))
            .hasToString("mreynolds")
    }

    @Test fun `Basic append`() {
        val b = FlavorableStringBuilder.fromString("Mal")
        b += " Reynolds"
        assert(b).all {
            hasToString("Mal Reynolds")
            hasLength(12)
        }
    }
}

private fun Assert<FlavorableCharSequence>.hasToString(string: String) {
    assert(actual.toString()).isEqualTo(string)
}
