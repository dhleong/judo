package net.dhleong.judo.input

import javax.swing.KeyStroke

/**
 * A sequence of KeyStrokes, useful for
 *  storing key mappings
 */
interface Keys : Collection<KeyStroke> {
    companion object {
        /**
         * Create an Keys from the given sequence
         */
        fun of(vararg strokes: KeyStroke): Keys = of(listOf(*strokes))

        fun of(strokes: List<KeyStroke>): Keys {
            val result = MutableKeys(strokes.size)
            strokes.forEach { result.push(it) }
            return result
        }

        fun parse(rawKeys: String): Keys {
            val parsed = mutableListOf<KeyStroke>()
            val buffer: StringBuilder by lazy { StringBuilder() }

            var inSpecial = false
            for (i in 0 until rawKeys.length) {
                if (rawKeys[i] == '<' && !inSpecial) {
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

            return of(parsed)
        }
    }

    operator fun get(index: Int): KeyStroke

    /**
     * @return a subset of the Keys in this sequence
     */
    fun slice(indices: IntRange): Keys

    fun clear()
}

/**
 * A mutable sequence of KeyStrokes. Useful for tracking input
 * @author dhleong
 */
class MutableKeys(initialCapacity: Int = 64) : Keys {

    private val strokes = ArrayList<KeyStroke>(initialCapacity)
    private var hash = 7

    override val size: Int
        get() = strokes.size

    override fun contains(element: KeyStroke): Boolean = strokes.contains(element)
    override fun containsAll(elements: Collection<KeyStroke>): Boolean = strokes.containsAll(elements)
    override operator fun get(index: Int): KeyStroke = strokes[index]
    override fun iterator(): Iterator<KeyStroke> = strokes.iterator()
    override fun isEmpty(): Boolean = strokes.isEmpty()

    override fun equals(other: Any?): Boolean =
        other is Keys
            && other.size == size
            && other.hashCode() == hash // bit of a hack

    override fun hashCode(): Int = hash

    fun push(stroke: KeyStroke) {
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

