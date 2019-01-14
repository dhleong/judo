package net.dhleong.judo.render

/**
 * A [FlavorableCharSequence] is a type of [CharSequence] that may have [Flavor]
 * applied to "spans" within the content.
 */
interface FlavorableCharSequence : CharSequence {
    val renderLength: Int
        get() {
            val len = length
            return when {
                (len > 0 && last() == '\n') -> len - 1
                else -> len
            }
        }

    fun codePointAt(index: Int): Int

    operator fun plusAssign(char: Char)
    operator fun plusAssign(other: FlavorableCharSequence)
    operator fun plus(string: String): FlavorableCharSequence

    fun splitAtNewlines(
        destination: MutableList<FlavorableCharSequence>,
        continueIncompleteLines: Boolean = false
    )

    override fun subSequence(startIndex: Int, endIndex: Int): FlavorableCharSequence

    fun isHidden(index: Int) = getFlavor(index).isHidden

    /**
     * Clear [Flavor] from the given range
     */
    fun clearFlavor(
        startIndex: Int,
        endIndex: Int
    )

    /**
     * Get the [Flavor] at the given index
     */
    fun getFlavor(index: Int): Flavor

    /**
     * Add the given [Flavor] to the given range
     */
    fun addFlavor(
        flavor: Flavor,
        startIndex: Int,
        endIndex: Int
    )

    /**
     * Begin the given [Flavor] at the given index, continuing until
     * either the end of this sequence or there's another [Flavor]
     * that overrides it
     */
    fun beginFlavor(
        flavor: Flavor,
        startIndex: Int
    )

    /**
     * Set the given [Flavor] on the given range. This replaces all
     * existing [Flavor] that may be there
     */
    fun setFlavor(
        flavor: Flavor,
        startIndex: Int,
        endIndex: Int = startIndex + 1
    )
}

inline fun FlavorableCharSequence.forEachChunk(
    start: Int = 0,
    end: Int = length,
    block: (startIndex: Int, endIndex: Int, flavor: Flavor) -> Unit
) {
    if (end - start < 0) throw IllegalArgumentException()
    if (end - start == 0) return

    var regionStart = start
    var lastFlavor = getFlavor(start)
    for (i in start + 1 until end) {
        val thisFlavor = getFlavor(i)
        if (thisFlavor != lastFlavor) {
            // new region!
            block(regionStart, i, lastFlavor)

            lastFlavor = thisFlavor
            regionStart = i
        }
    }

    block(regionStart, end, lastFlavor)
}