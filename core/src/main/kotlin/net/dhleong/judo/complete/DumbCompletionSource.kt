package net.dhleong.judo.complete

/**
 * The [DumbCompletionSource] simply stores completion
 * tokens in a sorted list and provides matching candidates
 * from that
 *
 * @author dhleong
 */
class DumbCompletionSource : CompletionSource {

    private val candidates = HashSet<String>(512)
    private val sortedList = ArrayList<String>(512)

    override fun process(string: CharSequence) {
        val before = candidates.size
        tokensFrom(string).forEach {
            val normalized = it.toLowerCase()
            if (!candidates.contains(normalized)) {
                candidates.add(normalized)
                sortedList.add(normalized)
            }
        }

        if (candidates.size > before) {
            sortedList.sort()
        }
    }

    override fun suggest(partial: CharSequence): Sequence<String> {
        var index = 0
        var foundAny = false
        val limit = sortedList.size
        return generateSequence {
            var result: String? = null
            while (index < limit) {
                val candidate = sortedList[index]
                ++index

                if (candidate.startsWith(partial, ignoreCase = true)) {
                    result = candidate
                    foundAny = true
                    break
                } else if (foundAny) {
                    // if we'd found some, there won't be any more now,
                    //  since the list is sorted
                    break
                }
            }

            result
        }
    }
}