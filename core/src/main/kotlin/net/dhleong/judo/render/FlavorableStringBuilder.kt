package net.dhleong.judo.render

/**
 * @author dhleong
 */
class FlavorableStringBuilder private constructor(
    private var chars: CharArray,
    private var flavor: Array<Flavor?>,
    private var start: Int = 0,
    private var myLength: Int = 0
): FlavorableCharSequence {

    constructor(capacity: Int) : this(
        CharArray(capacity),
        arrayOfNulls(capacity)
    )

    override fun plusAssign(other: FlavorableCharSequence) {
        ensureCapacity(myLength + other.length)

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subSequence(startIndex: Int, endIndex: Int): FlavorableCharSequence =
        FlavorableStringBuilder(chars, flavor, start + startIndex, endIndex - startIndex)

    override fun clearFlavor(startIndex: Int, endIndex: Int) {
        flavor.fill(Flavor.default, fromIndex = startIndex, toIndex = endIndex)
    }

    override fun getFlavor(index: Int): Flavor = flavor[start + index]
        ?: throw IllegalStateException("No flavor at $index")

    override fun removeFlavor(flavor: Flavor, startIndex: Int, endIndex: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addFlavor(flavor: Flavor, startIndex: Int, endIndex: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun beginFlavor(flavor: Flavor, startIndex: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setFlavor(flavor: Flavor, startIndex: Int, endIndex: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val length: Int
        get() = myLength

    override fun get(index: Int): Char = chars[start + index]

    private fun ensureCapacity(newCapacity: Int) {
        if (start + newCapacity <= chars.size) return // good to go

        val actualCapacity = newCapacity + 32
        val newChars = CharArray(actualCapacity)
        System.arraycopy(chars, start, newChars, 0, myLength)
        chars = newChars

        val newFlavors = arrayOfNulls<Flavor>(actualCapacity)
        System.arraycopy(flavor, start, newFlavors, 0, myLength)
        flavor = newFlavors
        start = 0

        // TODO fill the last flavor our to the capacity?
    }
}
