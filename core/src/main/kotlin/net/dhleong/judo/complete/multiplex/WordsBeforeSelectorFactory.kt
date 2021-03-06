package net.dhleong.judo.complete.multiplex

import net.dhleong.judo.complete.MultiplexSelectorFactory

/**
 * @author dhleong
 */
fun wordsBeforeFactory(wordCountFactory: (Int) -> WeightedRandomSelector): MultiplexSelectorFactory {
    return { string, wordRange ->

        val wordsBefore =
            // convenient shortcut
            if (wordRange.first == 0) 0

            // TODO: optimize and fix (double whitespace anyone?)
            else string.subSequence(0, wordRange.first)
                .count { Character.isWhitespace(it) }

        wordCountFactory(wordsBefore)
    }
}
