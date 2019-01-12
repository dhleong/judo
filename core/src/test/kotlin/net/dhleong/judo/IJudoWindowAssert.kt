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


