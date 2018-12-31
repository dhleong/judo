package net.dhleong.judo.render

/**
 * Flavor can be applied to spans within a [FlavorableCharSequence]
 */
interface Flavor {
    val isBold: Boolean
    val isFaint: Boolean
    val isItalic: Boolean
    val isUnderline: Boolean
    val isBlink: Boolean
    val isInverse: Boolean
    val isConceal: Boolean
    val isStrikeThrough: Boolean
    val isHidden: Boolean

    val hasForeground: Boolean
    val hasBackground: Boolean

    val foreground: JudoColor
    val background: JudoColor

    /**
     * Return a new Flavor that is the intersection of this object
     * with the given Flavor instance
     */
    operator fun minus(flavor: Flavor): Flavor

    /**
     * Combine this Flavor with another kind of Flavor
     */
    operator fun plus(flavor: Flavor): Flavor
}