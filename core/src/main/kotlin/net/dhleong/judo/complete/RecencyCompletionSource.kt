package net.dhleong.judo.complete

import net.dhleong.judo.util.ForgivingSequence

/**
 * @author dhleong
 */

class RecencyCompletionSource(
    val maxCandidates: Int = 5000
) : CompletionSource {

    private val candidates = LruCache<String, String>(maxCandidates)

    override fun process(string: CharSequence) {
        tokensFrom(string).forEach {
            val normalized = it.toLowerCase()

            candidates.put(normalized, it)
        }
    }

    override fun suggest(string: CharSequence, wordRange: IntRange): Sequence<String> {
        val partial = string.subSequence(wordRange)
        val partialLower = partial.toString().toLowerCase()
        return ForgivingSequence { candidates.reverseIterable }.filter {
            it.key.startsWith(partialLower)
        }.map {
            it.key // value?
        }
    }
}

internal class LruCache<K, V>(val maxCapacity: Int)
    // NOTE: loadFactor > 1f so we don't have to re-hash; since input history is
    // persisted, it is probably more likely that we will have large amount of
    // entries, so it makes sense not to waste allocations with a smaller array
    : LinkedHashMap<K, V>(maxCapacity * 3 / 4, 2f, /* accessOrder = */ true) {

    // NOTE: LinkedHashMap doesn't provide reverse iteration for some insane reason,
    // even though there's zero overhead. Sure, we could copy everything to a separate
    // ArrayList every time something changes, but... why? Let's just use reflection
    // and iterate ourselves. The overhead of reflection should be negligible here
    // since we're caching the Fields and not using them in a perf-sensitive path.
    val tailField = LinkedHashMap::class.java.getDeclaredField("tail")!!.apply {
        isAccessible = true
    }

    val nodeBeforeField = Class.forName("java.util.LinkedHashMap\$Entry")
        .getDeclaredField("before")!!.apply {
            isAccessible = true
        }

    val reverseIterable = Iterable {
        val tail = tailField.get(this)
        if (tail == null) emptySequence<Map.Entry<K, V>>().iterator()
        else ReverseIterator(tailField.get(this))
    }

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean =
        size > maxCapacity

    inner class ReverseIterator(tailNode: Any) : Iterator<Map.Entry<K, V>> {

        var next: Any? = tailNode

        override fun hasNext(): Boolean = next != null

        override fun next(): Map.Entry<K, V> {
            val result = next

            next = nodeBeforeField.get(result)

            @Suppress("UNCHECKED_CAST")
            return result as Map.Entry<K, V>
        }

    }
}
