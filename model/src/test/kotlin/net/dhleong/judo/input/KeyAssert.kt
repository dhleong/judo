package net.dhleong.judo.input

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions

/**
 * @author dhleong
 */
class KeyAssert(actual: Key)
    : AbstractAssert<KeyAssert, Key>(actual, KeyAssert::class.java) {

    fun hasChar(expected: Char): KeyAssert {
        isNotNull

        Assertions.assertThat(actual.char)
            .describedAs("char")
            .isEqualTo(expected)

        return myself
    }

    fun hasModifiers(vararg modifiers: Modifier): KeyAssert {
        isNotNull

        Assertions.assertThat(actual.modifiers)
            .containsExactlyInAnyOrder(*modifiers)

        return myself
    }

    fun hasKeyCode(expected: Int): KeyAssert {
        isNotNull

        Assertions.assertThat(actual.keyCode)
            .describedAs("keyCode")
            .isEqualTo(expected)

        return myself
    }
}

fun assertThat(actual: Key) = KeyAssert(actual)