package net.dhleong.judo.script

import net.dhleong.judo.net.AnsiFlavorableStringReader
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoAppendable

/**
 * @author dhleong
 */
fun List<String>.toFlavorableList(): List<FlavorableCharSequence> {
    // this is pretty inefficient, but it's not used super often...
    // we could be more efficient if we assumed script clients would
    // never use ansi, but... we shouldn't
    val ansiReader = AnsiFlavorableStringReader()
    return asSequence().flatMap { line ->
        line.toFlavorableSequence(ansiReader)
    }.toList()
}

fun String.toFlavorableSequence(
    ansiReader: AnsiFlavorableStringReader = AnsiFlavorableStringReader()
): Sequence<FlavorableCharSequence> =
    ansiReader.feed(toCharArray())

fun String.appendAsFlavorableTo(appendable: IJudoAppendable) {
    toFlavorableSequence().forEach {
        appendable.appendLine(it)
    }
}
