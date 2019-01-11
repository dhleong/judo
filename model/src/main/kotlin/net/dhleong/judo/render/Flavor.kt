package net.dhleong.judo.render

/**
 * Flavor can be applied to spans within a [FlavorableCharSequence]
 */
interface Flavor{
    val isBold: Boolean
    val isFaint: Boolean
    val isItalic: Boolean
    val isUnderline: Boolean
    val isBlink: Boolean
    val isInverse: Boolean
    val isConceal: Boolean
    val isStrikeThrough: Boolean
    val isHidden: Boolean

    val hasStyle: Boolean
        get() = isBold
            || isFaint
            || isItalic
            || isUnderline
            || isBlink
            || isInverse
            || isConceal
            || isStrikeThrough

    val hasForeground: Boolean
    val hasBackground: Boolean

    val foreground: JudoColor
    val background: JudoColor

    /**
     * Return a new Flavor that is the intersection of this object
     * with the given Flavor instance
     */
    operator fun minus(flavor: Flavor?): Flavor

    /**
     * Combine this Flavor with another kind of Flavor
     */
    operator fun plus(flavor: Flavor?): Flavor

    companion object {
        val default = SimpleFlavor()
    }
}

operator fun Flavor?.plus(other: Flavor?) =
    if (this == null) other
    else this + other

operator fun Flavor?.minus(other: Flavor?) =
    if (this == null) Flavor.default
    else this - other

// TODO: we could implement Flavor as an inline class using a representation
// like JLine's and be much more memory efficient...
data class SimpleFlavor(
    override var isBold: Boolean = false,
    override var isFaint: Boolean = false,
    override var isItalic: Boolean = false,
    override var isUnderline: Boolean = false,
    override var isBlink: Boolean = false,
    override var isInverse: Boolean = false,
    override var isConceal: Boolean = false,
    override var isStrikeThrough: Boolean = false,
    override var isHidden: Boolean = false,

    override var hasForeground: Boolean = false,
    override var hasBackground: Boolean = false,

    override var foreground: JudoColor = JudoColor.Default,
    override var background: JudoColor = JudoColor.Default
) : Flavor {
    constructor(other: Flavor) : this(
        isBold = other.isBold,
        isFaint = other.isFaint,
        isItalic = other.isItalic,
        isUnderline = other.isUnderline,
        isBlink = other.isBlink,
        isInverse = other.isInverse,
        isConceal = other.isConceal,
        isStrikeThrough = other.isStrikeThrough,
        isHidden = other.isHidden,

        hasForeground = other.hasForeground,
        hasBackground = other.hasBackground,

        foreground = other.foreground,
        background = other.background
    )

    override operator fun minus(flavor: Flavor?): Flavor {
        if (this == flavor) return Flavor.default

        TODO()
    }

    override operator fun plus(flavor: Flavor?): Flavor {
        if (this == flavor) return this
        return SimpleFlavor(
            isBold = isBold || flavor?.isBold ?: false,
            isFaint = isFaint || flavor?.isFaint ?: false,
            isItalic = isItalic || flavor?.isItalic ?: false,
            isUnderline = isUnderline || flavor?.isUnderline ?: false,
            isBlink = isBlink || flavor?.isBlink ?: false,
            isInverse = isInverse || flavor?.isInverse ?: false,
            isConceal = isConceal || flavor?.isConceal ?: false,
            isStrikeThrough = isStrikeThrough || flavor?.isStrikeThrough ?: false,
            isHidden = isHidden || flavor?.isHidden ?: false,

            hasForeground = hasForeground || flavor?.hasForeground ?: false,
            hasBackground = hasBackground || flavor?.hasBackground ?: false,

            foreground = flavor?.let { other ->
                foreground + other.foreground
            } ?: foreground,
            background = flavor?.let { other ->
                background + other.background
            } ?: background
        )
    }

    fun reset() {
        isBold = false
        isFaint = false
        isItalic = false
        isUnderline = false
        isBlink = false
        isInverse = false
        isConceal = false
        isStrikeThrough = false
        isHidden = false

        hasForeground = false
        hasBackground = false

        foreground = JudoColor.Default
        background = JudoColor.Default
    }
}
