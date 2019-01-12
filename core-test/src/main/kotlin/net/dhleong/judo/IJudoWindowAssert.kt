package net.dhleong.judo

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.render.IJudoWindow2

/**
 * @author dhleong
 */
fun Assert<IJudoWindow2>.hasHeight(height: Int) {
    if (actual.height == height) return
    expected("height = ${show(height)} but was ${show(actual.height)}")
}

