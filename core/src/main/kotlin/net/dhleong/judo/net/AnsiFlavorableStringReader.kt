package net.dhleong.judo.net

import net.dhleong.judo.render.FlavorableCharSequence

/**
 * Given sequences of ANSI text (IE from the network) produces
 * [FlavorableCharSequence] instances. Newlines will be included
 * at the end of complete lines.
 *
 * @author dhleong
 */
class AnsiFlavorableStringReader {

    fun feed(chars: CharArray, available: Int): Sequence<FlavorableCharSequence> = sequence {
        // TODO
    }

}