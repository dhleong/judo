package net.dhleong.judo.complete

/**
 * A Markov-chain-inspired CompletionSource. This is made up of
 * multiple "layers." The N'th "layer" consists of a list of
 * words at the N'th position of an input, each with links to
 * a word in the (N+1)'th layer, based on how many times such
 * a transition occurred. Words after the M'th layer are ignored.
 *
 * @param stopWords A set of words that, when encountered while processing,
 *  will immediately stop further processing (IE: they will never be in the trie)
 *
 * @author dhleong
 */
class MarkovCompletionSource(stopWords: Set<String> = emptySet()) : CompletionSource {
    private val trie = MarkovTrie(stopWords = stopWords)

    override fun process(string: CharSequence) {
        trie.add(tokensFrom(string, anyLength = true).map { it.toLowerCase() })
    }

    override fun suggest(string: CharSequence, wordRange: IntRange): Sequence<String> {
        val partial = string.subSequence(wordRange)
        val partialLower = partial.toString().toLowerCase()
        val tokensBefore = tokensFrom(
            string.subSequence(0, wordRange.first),
            anyLength = true
        ).map { it.toLowerCase() }

        return trie.query(tokensBefore).filter {
            it.value.startsWith(partialLower)
        }.sortedByDescending { it.incomingCount }
         .map { it.value }
    }
}

class MarkovTrie<T>(
    private val maxDepth: Int = 5,
    private val stopWords: Set<T> = emptySet()
) {

    private val root = EmptyMarkovNode<T>()

    fun add(sequence: Sequence<T>) {
        val iterator = sequence.iterator()
        if (!iterator.hasNext()) return

        root.add(iterator, stopWords, remainingCount = maxDepth)
    }

    fun query(sequence: Sequence<T>): Sequence<MarkovNode<T>> =
        root.query(sequence.iterator())
}

open class EmptyMarkovNode<T> internal constructor() {
    val transitions = HashMap<T, MarkovNode<T>>()

    fun add(sequence: Iterator<T>, stopWords: Set<T>, remainingCount: Int) {
        val nextValue = sequence.next()
        if (nextValue in stopWords) return

        val transition = getOrMakeNextNode(nextValue)
        ++transition.incomingCount

        val newRemainingCount = remainingCount - 1
        if (newRemainingCount > 0 && sequence.hasNext()) {
            transition.add(sequence, stopWords, newRemainingCount)
        }
    }

    fun query(sequence: Iterator<T>): Sequence<MarkovNode<T>> {
        if (!sequence.hasNext()) {
            // we're the end of the line! return all of our transitions
            return transitions.values.asSequence()
        }

        val next = sequence.next()
        transitions[next]?.let {
            return it.query(sequence)
        }

        // no more matching nodes? give up
        return emptySequence()
    }

    private fun getOrMakeNextNode(value: T): MarkovNode<T> {
        val existing = transitions[value]
        if (existing != null) return existing

        val newNode = MarkovNode(value)
        transitions[value] = newNode
        return newNode
    }
}

class MarkovNode<T>(val value: T) : EmptyMarkovNode<T>() {
    var incomingCount = 0
}

