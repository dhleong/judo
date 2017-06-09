package net.dhleong.judo.complete

/**
 * @author dhleong
 */

interface CompletionSource {
    /**
     * Process a string for possible completions
     */
    fun process(string: CharSequence)

    /**
     * Given an input string and an IntRange, representing
     * the word to be completed, return a lazy Sequence of
     * suggested completions
     */
    fun suggest(string: CharSequence, wordRange: IntRange): Sequence<String>

    /**
     * Given a partial string, return a lazy Sequence of
     * suggested completions for that "word"
     */
    fun suggest(partial: CharSequence) =
        suggest(partial, 0..partial.lastIndex)
}

class CompletionSourceFacade {
    companion object {
        fun create(): CompletionSource {
            // no config to speak of...
            return RecencyCompletionSource()
        }
    }
}
