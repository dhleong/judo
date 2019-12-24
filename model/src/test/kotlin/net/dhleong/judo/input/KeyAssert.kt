package net.dhleong.judo.input

import assertk.Assert
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo

fun Assert<Key>.hasChar(expected: Char) = given { actual ->
    assertThat(actual.char, "char").isEqualTo(expected)
}

fun Assert<Key>.hasModifiers(vararg modifiers: Modifier) = given { actual ->
    assertThat(actual.modifiers, "modifiers")
        .containsOnly(*modifiers)
}

fun Assert<Key>.hasKeyCode(expected: Int) = given { actual ->
    assertThat(actual.keyCode, "keyCode").isEqualTo(expected)
}
