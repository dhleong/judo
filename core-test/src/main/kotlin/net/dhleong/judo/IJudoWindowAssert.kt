package net.dhleong.judo

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.render.IJudoWindow

/**
 * @author dhleong
 */
fun Assert<IJudoWindow>.hasHeight(height: Int) {
    if (actual.height == height) return
    expected("height = ${show(height)} but was ${show(actual.height)}")
}

fun Assert<IJudoWindow>.isFocused() {
    if (actual.isFocused) return
    expected("to be focused, but was not")
}

fun Assert<IJudoWindow>.isNotFocused() {
    if (!actual.isFocused) return
    expected("to NOT be focused, but *was*")
}
