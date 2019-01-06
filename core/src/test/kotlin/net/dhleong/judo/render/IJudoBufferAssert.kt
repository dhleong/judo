package net.dhleong.judo.render

import assertk.Assert
import assertk.assertions.isEqualTo

/**
 * @author dhleong
 */
fun Assert<IJudoBuffer2>.hasLines(vararg expectedStrings: String) {
    val actualStrings = (0..actual.lastIndex).map { actual[it].toString() }
    assert(actualStrings, "buffer lines").isEqualTo(expectedStrings.toList())
}