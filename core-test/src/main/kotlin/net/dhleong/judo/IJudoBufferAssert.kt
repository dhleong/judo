package net.dhleong.judo

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoBuffer

/**
 * @author dhleong
 */
fun Assert<IJudoBuffer>.hasLines(vararg expectedStrings: String) {
    val actualStrings = (0..actual.lastIndex).map { actual[it].toString() }
    assert(actualStrings, "buffer lines").isEqualTo(expectedStrings.toList())
}
fun Assert<IJudoBuffer>.hasLinesSomewhere(vararg expectedStrings: String) {
    val actualStrings = (0..actual.lastIndex).map { actual[it].toString() }
    val firstIndex = actualStrings.indexOf(expectedStrings[0])
    if (firstIndex != -1) {
        val subset = actualStrings.subList(firstIndex, firstIndex + expectedStrings.size)
        assert(subset, "buffer lines").isEqualTo(expectedStrings.toList())
        return
    }

    expected("buffer of ${show(actualStrings)} to contain the sequence: ${show(expectedStrings)}")
}

fun Assert<IJudoBuffer>.doesNotHaveLine(line: FlavorableCharSequence) {
    for (i in 0..actual.lastIndex) {
        assert(actual[i] != line, "line $i doesn't match `$line`").isTrue()
    }
}

fun Assert<IJudoBuffer>.hasSize(size: Int) {
    if (actual.size == size) return
    expected("size = ${show(size)} but was ${show(actual.size)}")
}
