package net.dhleong.judo.render

/**
 * A [FlavorableCharSequence] is a type of [CharSequence] that may have [Flavor]
 * applied to "spans" within the content.
 */
interface FlavorableCharSequence : CharSequence {

    override fun subSequence(startIndex: Int, endIndex: Int): FlavorableCharSequence

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
     * Remove the given [Flavor] from the given range
     */
    fun removeFlavor(
        flavor: Flavor,
        startIndex: Int,
        endIndex: Int
    )

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
        endIndex: Int
    )

}