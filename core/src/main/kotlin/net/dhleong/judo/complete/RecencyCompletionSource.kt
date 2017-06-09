package net.dhleong.judo.complete

import net.dhleong.judo.util.removeWhile
import java.util.TreeMap

/**
 * @author dhleong
 */

class CompletionCandidate(
    val word: String,
    val lastReference: Long = System.currentTimeMillis()
) : Comparable<CompletionCandidate> {

    override fun equals(other: Any?): Boolean {
        return other is CompletionCandidate
            && word == other.word
    }

    override fun hashCode(): Int = word.hashCode()

    override fun compareTo(other: CompletionCandidate): Int {
        val byTime = other.lastReference.compareTo(lastReference)
        if (byTime != 0) return byTime
        return word.compareTo(other.word)
    }

    override fun toString(): String =
        "Candidate(word='$word')"
}

class RecencyCompletionSource(
    val maxCandidates: Int = 5000,
    val timer: () -> Long = System::currentTimeMillis
) : CompletionSource {

    private val candidates = TreeMap<CompletionCandidate, CompletionCandidate>()

    override fun process(string: CharSequence) {
        tokensFrom(string).forEach {
            val now = timer()
            val normalized = it.toLowerCase()

            // we can't query by string, and even if there was an
            // older Candidate for this string, we wouldn't find it
            // due to primarily sorting by time. So, don't bother
            // searching and just always put(), then just dedup
            // in suggest()
            val candidate = CompletionCandidate(normalized, now)
            candidates.put(candidate, candidate)
        }

        // prune
        val max = maxCandidates
        val oldest = candidates.descendingKeySet()
        oldest.removeWhile { candidates.size > max }
    }

    override fun suggest(string: CharSequence, wordRange: IntRange): Sequence<String> {
        // FIXME if we have to process() while iterating over this,
        // a ConcurrentModificationException is thrown
        val partial = string.subSequence(wordRange)
        val partialLower = partial.toString().toLowerCase()
        return candidates.keys.asSequence().filter {
            it.word.startsWith(partialLower)
        }.distinct() // TODO can/should we prune dups from the map?
         .map { it.word }
    }
}
