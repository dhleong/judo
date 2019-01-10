package net.dhleong.judo

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IJudoWindow2
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

fun Assert<IJudoWindow2>.hasHeight(expected: Int) {
    if (actual.height == expected) return
    expected("height=${show(expected)} but was ${show(actual.height)}")
}

fun IJudoWindow.getDisplayStrings(): List<String> =
    with(mutableListOf<CharSequence>()) {
        getDisplayLines(this)
        map { (it as AttributedString).toAnsi() }
    }
