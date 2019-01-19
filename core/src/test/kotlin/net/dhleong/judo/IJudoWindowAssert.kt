package net.dhleong.judo

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.render.IJudoWindow

/**
 * @author dhleong
 */
fun Assert<IJudoWindow>.hasHeight(expected: Int) {
    if (actual.height == expected) return
    expected("height=${show(expected)} but was ${show(actual.height)}")
}

fun Assert<IJudoWindow>.hasId(expected: Int) {
    if (actual.id == expected) return
    expected("id=${show(expected)} but was ${show(actual.id)}")
}
