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
     * Given a partial input string, return a lazy
     * Sequence of suggested completions
     */
    fun suggest(partial: CharSequence): Sequence<String>
}

class CompletionSourceFacade {
    companion object {
        fun create(): CompletionSource {
            // right now we just have the DumbCompletionSource,
            // and don't support any config...
            return DumbCompletionSource()
        }
    }
}
