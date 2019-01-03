package net.dhleong.judo.render

/**
 * @author dhleong
 */
class FlavorableStringBuilder private constructor(
    private var chars: CharArray,
    private var flavors: Array<Flavor?>,
    private var start: Int = 0,
    private var myLength: Int = 0
): FlavorableCharSequence {

    constructor(capacity: Int) : this(
        CharArray(capacity),
        arrayOfNulls(capacity)
    )

    override fun codePointAt(index: Int) = Character.codePointAt(chars, index)

    fun append(string: String, offset: Int, length: Int) {
        ensureCapacity(myLength + length)

        string.toCharArray(chars, start + myLength, startIndex = offset)
        if (isNotEmpty()) {
            flavors.fill(flavors[start + myLength - 1], fromIndex = start + offset + myLength)
        }

        myLength += length
    }

    operator fun plusAssign(string: String) {
        append(string, 0, string.length)
    }

    fun append(other: FlavorableCharSequence, offset: Int, length: Int) {
        ensureCapacity(myLength + length)

        if (other is FlavorableStringBuilder) {
            System.arraycopy(other.chars, other.start + offset, chars, start + myLength, length)
            System.arraycopy(other.flavors, other.start + offset, flavors, start + myLength, length)

            val newStartingFlavor = flavors[start + myLength - 1] + other.flavors[other.start + offset]
            if (newStartingFlavor != null) {
                beginFlavor(newStartingFlavor, myLength)
            }
        } else {
            TODO("not implemented")
        }

        myLength += length
    }

    override fun plusAssign(other: FlavorableCharSequence) {
        append(other, 0, other.length)
    }

    override fun subSequence(startIndex: Int, endIndex: Int): FlavorableStringBuilder =
        FlavorableStringBuilder(chars, flavors, start + startIndex, endIndex - startIndex)

    override fun clearFlavor(startIndex: Int, endIndex: Int) {
        flavors.fill(Flavor.default, fromIndex = startIndex, toIndex = endIndex)
    }

    override fun getFlavor(index: Int): Flavor = flavors[start + index]
        ?: throw IllegalStateException("No flavors at $index")

    override fun addFlavor(flavor: Flavor, startIndex: Int, endIndex: Int) {
        flavors.forEachIndexed { index, old ->
            flavors[index] = old + flavor
        }
    }

    override fun beginFlavor(flavor: Flavor, startIndex: Int) {
        for (i in start + startIndex until flavors.size) {
            if ((flavors[i] ?: flavor) != flavor) {
                break
            }

            flavors[i] = flavor
        }
    }

    override fun setFlavor(flavor: Flavor, startIndex: Int, endIndex: Int) {
        flavors.fill(
            flavor,
            fromIndex = start + startIndex,
            toIndex = start + endIndex
        )
    }

    override val length: Int
        get() = myLength

    override fun get(index: Int): Char = chars[start + index]

    override fun toString(): String = String(chars, start, myLength)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is FlavorableCharSequence) return false
        if (myLength != other.length) return false

        for (i in 0 until myLength) {
            if (this[i] != other[i]) return false
            if (getFlavor(i) != other.getFlavor(i)) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = myLength
        for (i in start until start + myLength) {
            result = 31 * result + chars[i].hashCode()
            result = 31 * result + flavors[i].hashCode()
        }
        return result
    }


    private fun ensureCapacity(newCapacity: Int) {
        if (start + newCapacity <= chars.size) return // good to go

        val actualCapacity = newCapacity + 32
        val newChars = CharArray(actualCapacity)
        System.arraycopy(chars, start, newChars, 0, myLength)
        chars = newChars

        val newFlavors = arrayOfNulls<Flavor>(actualCapacity)
        System.arraycopy(flavors, start, newFlavors, 0, myLength)
        flavors = newFlavors
        start = 0

        // TODO fill the last flavor up to the capacity?
    }

    companion object {
        val EMPTY: FlavorableCharSequence = fromString("")

        fun fromString(string: String) = FlavorableStringBuilder(string.length).also {
            it += string
        }

        fun withDefaultFlavor(string: String) = fromString(string).apply {
            beginFlavor(Flavor.default, 0)
        }
    }
}
