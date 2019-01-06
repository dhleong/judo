package net.dhleong.judo.script

import net.dhleong.judo.net.AnsiFlavorableStringReader
import net.dhleong.judo.render.FlavorableCharSequence

/**
 * @author dhleong
 */
fun List<String>.toFlavorableList(): List<FlavorableCharSequence> {
    // this is pretty inefficient, but it's not used super often...
    // we could be more efficient if we assumed script clients would
    // never use ansi, but... we shouldn't
    val ansiReader = AnsiFlavorableStringReader()
    return asSequence().flatMap { line ->
        ansiReader.feed(line.toCharArray())
    }.toList()
}