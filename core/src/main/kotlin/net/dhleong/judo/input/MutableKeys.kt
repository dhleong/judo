package net.dhleong.judo.input

/**
 * A sequence of Keys, useful for
 *  storing key mappings
 */
interface Keys : Collection<Key> {
    companion object {
        /**
         * Create an Keys from the given sequence
         */
        fun of(vararg strokes: Key): Keys = of(listOf(*strokes))

        fun of(strokes: List<Key>): Keys {
            val result = MutableKeys(strokes.size)
            strokes.forEach { result.push(it) }
            return result
        }

        fun parse(rawKeys: String): Keys {
            val parsed = mutableListOf<Key>()
            val buffer: StringBuilder by lazy { StringBuilder() }

            var inSpecial = false
            for (i in rawKeys.indices) {
                if (!inSpecial && startSpecial(rawKeys, i)) {
                    inSpecial = true
                } else if (inSpecial && rawKeys[i] == '>' && buffer[buffer.length-1] != '\\') {
                    // parse special character in `buffer`
                    parsed.add(key(buffer.toString()))

                    // reset
                    inSpecial = false
                    buffer.setLength(0)
                } else if (inSpecial && rawKeys[i] == '>') {
                    // escaped >
                    buffer[buffer.length-1] = '>'
                } else if (inSpecial) {
                    // any other char
                    buffer.append(rawKeys[i])
                } else {
                    parsed.add(key("${rawKeys[i]}"))
                }
            }

            if (inSpecial) {
                parsed.add(key("<"))
            }

            return of(parsed)
        }

        private fun startSpecial(rawKeys: String, i: Int): Boolean {
            // special char only starts on `<`
            if (rawKeys[i] != '<') return false

            // `<` must not have been escaped
            if (i > 0 && rawKeys[i - 1] == '\\') return false

            // look for a matching `>`; we start from 2 chars after
            // the `<` to handle special case `<>`, which is probably
            // not intended to be a special char sequence
            @Suppress("LoopToCallChain")
            for (j in (i + 2)..rawKeys.lastIndex) {
                if (rawKeys[j] == '>' && rawKeys[j - 1] != '\\') {
                    // we found a matching, non-escaped `>`;
                    // this is a legit special char sequence
                    return true
                }
            }

            return false
        }
    }

    operator fun get(index: Int): Key

    /**
     * @return a subset of the Keys in this sequence
     */
    fun slice(indices: IntRange): Keys

    fun clear()

    fun describeTo(out: Appendable)
}

/**
 * A mutable sequence of Keys. Useful for tracking input
 * @author dhleong
 */
class MutableKeys(initialCapacity: Int = 64) : Keys {

    private val strokes = ArrayList<Key>(initialCapacity)
    private var hash = 7

    override val size: Int
        get() = strokes.size

    override fun contains(element: Key): Boolean = strokes.contains(element)
    override fun containsAll(elements: Collection<Key>): Boolean = strokes.containsAll(elements)
    override operator fun get(index: Int): Key = strokes[index]
    override fun iterator(): Iterator<Key> = strokes.iterator()
    override fun isEmpty(): Boolean = strokes.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (other !is Keys) return false
        if (!(other.size == size
                && other.hashCode() == hash)) {
            // quick reject
            return false
        }

        // okay, now make very certain
        // we don't use .none{} or .all{} here because
        // they create an iterator, and typing happens
        // a lot. We could *probably* handle the garbage,
        // but... this is still pretty readable.
        @Suppress("LoopToCallChain")
        for (i in indices) {
            if (this[i] != other[i]) return false
        }

        // hurray!
        return true
    }

    override fun hashCode(): Int = hash

    override fun describeTo(out: Appendable) =
        strokes.forEach { it.describeTo(out) }

    fun push(stroke: Key) {
        strokes.add(stroke)
        hash += 31 * stroke.hashCode()
    }

    override fun slice(indices: IntRange): Keys = Keys.of(strokes.slice(indices))

    override fun clear() {
        strokes.clear()
        hash = 7
    }

    override fun toString(): String {
        return strokes.toString()
    }
}

