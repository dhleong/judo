package net.dhleong.judo.complete

interface MultiplexSelector {
    /**
     * The candidates will be in order by the input sources.
     * An empty string should be ignored
     *
     * @return The Index of the best candidate
     */
    fun select(candidates: List<String>): Int
}

/**
 * [MultiplexCompletionSource] combines several sources and provides
 * a single interface across them, switching between them using some
 * scoring algorithm
 *
 * @param selectorFactory Called on [suggest] with the same arguments
 * @author dhleong
 */
class MultiplexCompletionSource(
    val sources: List<CompletionSource>,
    val selectorFactory: (string: CharSequence, wordRange: IntRange) -> MultiplexSelector
) : CompletionSource {

    override fun process(string: CharSequence) {
        // NOTE: it will probably be more beneficial for each source
        // to process different things, but let's support everything
        // processing the same something
        sources.forEach { it.process(string) }
    }

    override fun suggest(string: CharSequence, wordRange: IntRange): Sequence<String> {
        val sourceSequences = sources.map { it.suggest(string, wordRange).iterator() }
        val count = sourceSequences.size
        val workspace = ArrayList<String>(count)
        var currentEmpty = 0

        @Suppress("LoopToCallChain")
        for (i in 0..count-1) {
            val seq = sourceSequences[i]
            if (seq.hasNext()) {
                workspace.add(seq.next())
            } else {
                workspace.add("") // none to start with
                ++currentEmpty
            }
        }

        val selector = selectorFactory(string, wordRange)
        var lastSelected = -1
        return generateSequence {
            // advance the last-selected iterator
            if (lastSelected != -1) {
                val seq = sourceSequences[lastSelected]
                if (!seq.hasNext()) {
                    // no more; this should only be called once,
                    // since it can never become lastSelected again
                    workspace[lastSelected] = ""
                    ++currentEmpty
                } else {
                    workspace[lastSelected] = seq.next()
                }
            }

            if (currentEmpty >= count) {
                // we're empty
                null
            } else {
                // select the best one
                val selected = selector.select(workspace)
                lastSelected = selected

                // return it
                workspace[selected]
            }
        }
    }
}
