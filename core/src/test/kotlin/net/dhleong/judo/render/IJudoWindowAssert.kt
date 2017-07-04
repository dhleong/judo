package net.dhleong.judo.render

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.jline.utils.AttributedString

/**
 * @author dhleong
 */
class IJudoWindowAssert(actual: IJudoWindow?)
    : AbstractAssert<IJudoWindowAssert, IJudoWindow>(actual, IJudoWindowAssert::class.java) {

    fun hasHeight(expected: Int): IJudoWindowAssert {
        isNotNull

        assertThat(actual.height)
            .describedAs("height")
            .isEqualTo(expected)

        return myself
    }

    fun displaysStrings(vararg expected: String): IJudoWindowAssert {
        isNotNull

        assertThat(actual.getDisplayStrings())
            .containsExactly(*expected)

        return myself
    }
}

fun IJudoWindow.getDisplayStrings(): List<String> =
    with(mutableListOf<CharSequence>()) {
        getDisplayLines(this)
        map { (it as AttributedString).toAnsi() }
    }
