package net.dhleong.judo.logging

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Java6Assertions

/**
 * @author dhleong
 */

internal class AnsiStylistAssert(actual: AnsiStylist)
    : AbstractAssert<AnsiStylistAssert, AnsiStylist>(actual, AnsiStylistAssert::class.java) {

    fun hasBg(expected: Int): AnsiStylistAssert {
        Java6Assertions.assertThat(actual.bg)
            .describedAs("bg")
            .isEqualTo(expected)
        return myself
    }

    fun hasNoBg(): AnsiStylistAssert {
        Java6Assertions.assertThat(actual.bg)
            .isEqualTo(-1)
            .overridingErrorMessage("Expected to have no specified bg, but was ${actual.bg}")
        return myself
    }


    fun hasFg(expected: Int): AnsiStylistAssert {
        Java6Assertions.assertThat(actual.fg)
            .describedAs("fg")
            .isEqualTo(expected)
        return myself
    }

    fun hasNoFg(): AnsiStylistAssert {
        Java6Assertions.assertThat(actual.fg)
            .isEqualTo(-1)
            .overridingErrorMessage("Expected to have no specified fg, but was ${actual.fg}")
        return myself
    }


    fun isBold(): AnsiStylistAssert {
        Java6Assertions.assertThat(actual.bold)
            .describedAs("bold")
            .isEqualTo(true)
        return myself
    }

    fun isNotBold(): AnsiStylistAssert {
        Java6Assertions.assertThat(actual.bold)
            .describedAs("bold")
            .isEqualTo(false)
        return myself
    }


    fun isItalic(): AnsiStylistAssert {
        Java6Assertions.assertThat(actual.bold)
            .describedAs("italic")
            .isEqualTo(true)
        return myself
    }

    fun isNotItalic(): AnsiStylistAssert {
        Java6Assertions.assertThat(actual.bold)
            .describedAs("italic")
            .isEqualTo(false)
        return myself
    }

}
