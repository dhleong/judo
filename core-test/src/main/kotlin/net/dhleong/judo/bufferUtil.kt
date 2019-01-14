package net.dhleong.judo

import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer

/**
 * @author dhleong
 */
fun emptyBuffer() = JudoBuffer(IdManager())
fun bufferOf(contents: String) = emptyBuffer().apply {
    contents.split("\n").forEach {
        appendLine(FlavorableStringBuilder.withDefaultFlavor(it))
    }
}

fun bufferOf(vararg lines: FlavorableCharSequence) = emptyBuffer().apply {
    set(lines.toList())
}
