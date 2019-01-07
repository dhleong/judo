package net.dhleong.judo.render

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import assertk.assertions.support.show

/**
 * @author dhleong
 */
fun Assert<IJudoBuffer2>.hasLines(vararg expectedStrings: String) {
    val actualStrings = (0..actual.lastIndex).map { actual[it].toString() }
    assert(actualStrings, "buffer lines").isEqualTo(expectedStrings.toList())
}
fun Assert<IJudoBuffer2>.hasLinesSomewhere(vararg expectedStrings: String) {
    val actualStrings = (0..actual.lastIndex).map { actual[it].toString() }
    val firstIndex = actualStrings.indexOf(expectedStrings[0])
    if (firstIndex != -1) {
        val subset = actualStrings.subList(firstIndex, firstIndex + expectedStrings.size)
        assert(subset, "buffer lines").isEqualTo(expectedStrings.toList())
        return
    }

    expected("buffer of ${show(actualStrings)} to contain the sequence: ${show(expectedStrings)}")
}
