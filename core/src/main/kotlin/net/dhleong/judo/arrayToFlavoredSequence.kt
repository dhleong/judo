package net.dhleong.judo

import net.dhleong.judo.net.AnsiFlavorableStringReader
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder

/**
 * @author dhleong
 */
private val arrayToSequenceAnsiParser = AnsiFlavorableStringReader()
internal fun Array<out Any?>.toFlavoredSequence(): FlavorableCharSequence {
    val joined = FlavorableStringBuilder(16)

    var first = true
    for (o in this) {
        if (first) first = false
        else joined += " "

        when (o) {
            is FlavorableCharSequence -> joined += o
            is String -> {
                val chars = o.toCharArray()
                arrayToSequenceAnsiParser.reset()
                for (fcs in arrayToSequenceAnsiParser.feed(chars)) {
                    joined += fcs
                }
            }
            else -> joined += o.toString()
        }
    }

    return joined
}

