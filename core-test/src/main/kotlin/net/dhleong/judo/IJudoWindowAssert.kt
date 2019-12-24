package net.dhleong.judo

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.render.IJudoWindow

/**
 * @author dhleong
 */
fun Assert<IJudoWindow>.hasHeight(height: Int) = given { actual ->
    if (actual.height == height) return
    expected("height = ${show(height)} but was ${show(actual.height)}")
}

fun Assert<IJudoWindow>.hasWidth(width: Int) = given { actual ->
    if (actual.width == width) return
    expected("width = ${show(width)} but was ${show(actual.width)}")
}

fun Assert<IJudoWindow>.isFocused() = given { actual ->
    if (actual.isFocused) return
    expected("to be focused, but was not")
}

fun Assert<IJudoWindow>.isNotFocused() = given { actual ->
    if (!actual.isFocused) return
    expected("to NOT be focused, but *was*")
}

fun Assert<IJudoWindow>.hasId(expected: Int) = given { actual ->
    if (actual.id == expected) return
    expected("id=${show(expected)} but was ${show(actual.id)}")
}
