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

    operator fun plusAssign(string: String) {
        ensureCapacity(myLength + string.length)

        string.toCharArray(chars, start + myLength)
        if (isNotEmpty()) {
            flavors.fill(flavors[start + myLength - 1], fromIndex = start + myLength)
        }

        myLength += string.length
    }

    override fun plusAssign(other: FlavorableCharSequence) {
        ensureCapacity(myLength + other.length)

        if (other is FlavorableStringBuilder) {
            System.arraycopy(other.chars, other.start, chars, start + myLength, other.myLength)
            System.arraycopy(other.flavors, other.start, flavors, start + myLength, other.myLength)

            val newStartingFlavor = getFlavor(lastIndex) + other.flavors[other.start]
            beginFlavor(newStartingFlavor, myLength)
        } else {
            TODO("not implemented")
        }

        myLength += other.length
    }

    override fun subSequence(startIndex: Int, endIndex: Int): FlavorableCharSequence =
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
        fun fromString(string: String) = FlavorableStringBuilder(string.length).also {
            it += string
        }
    }
}
