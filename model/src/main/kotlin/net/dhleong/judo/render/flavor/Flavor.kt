package net.dhleong.judo.render.flavor

import net.dhleong.judo.render.JudoColor

/**
 * Flavor can be applied to spans within a
 * [net.dhleong.judo.render.FlavorableCharSequence]
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

    companion object {
        val default = flavor()
    }
}

operator fun Flavor.plus(flavor: Flavor?): Flavor {
    if (this == flavor) return this
    return flavor(
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

fun flavor(
    isBold: Boolean = false,
    isFaint: Boolean = false,
    isItalic: Boolean = false,
    isUnderline: Boolean = false,
    isBlink: Boolean = false,
    isInverse: Boolean = false,
    isConceal: Boolean = false,
    isStrikeThrough: Boolean = false,
    isHidden: Boolean = false,

    hasForeground: Boolean = false,
    hasBackground: Boolean = false,

    foreground: JudoColor = JudoColor.Default,
    background: JudoColor = JudoColor.Default
): Flavor = when {
    // prefer IntFlavor where possible
    (
        foreground !is JudoColor.FullRGB
            && background !is JudoColor.FullRGB
    ) -> IntFlavor(
        isBold = isBold,
        isFaint = isFaint,
        isItalic = isItalic,
        isUnderline = isUnderline,
        isBlink = isBlink,
        isInverse = isInverse,
        isConceal = isConceal,
        isStrikeThrough = isStrikeThrough,
        isHidden = isHidden,

        hasForeground = hasForeground,
        hasBackground = hasBackground,

        foreground = foreground,
        background = background
    )

    // fallback to ExplicitFlavor
    else -> WrappedFlavor(IntFlavor(
        isBold = isBold,
        isFaint = isFaint,
        isItalic = isItalic,
        isUnderline = isUnderline,
        isBlink = isBlink,
        isInverse = isInverse,
        isConceal = isConceal,
        isStrikeThrough = isStrikeThrough,
        isHidden = isHidden,

        hasForeground = hasForeground,
        hasBackground = hasBackground
    ), fg = foreground, bg = background)
}

fun Flavor.copy(): Flavor = when {
    // copying a Wrapped or IntFlavor is pointless; they're immutable
    this is IntFlavor -> this
    this is WrappedFlavor -> this

    (
        foreground !is JudoColor.FullRGB
            && background !is JudoColor.FullRGB
    ) -> IntFlavor(this)

    else -> flavor(
        isBold = isBold,
        isFaint = isFaint,
        isItalic = isItalic,
        isUnderline = isUnderline,
        isBlink = isBlink,
        isInverse = isInverse,
        isConceal = isConceal,
        isStrikeThrough = isStrikeThrough,
        isHidden = isHidden,

        hasForeground = hasForeground,
        hasBackground = hasBackground,

        foreground = foreground,
        background = background
    )
}

fun areFlavorsEqual(a: Flavor, other: Any?): Boolean {
    if (a === other) return true
    if (other !is Flavor) return false

    if (a.isBold != other.isBold) return false
    if (a.isFaint != other.isFaint) return false
    if (a.isItalic != other.isItalic) return false
    if (a.isUnderline != other.isUnderline) return false
    if (a.isBlink != other.isBlink) return false
    if (a.isInverse != other.isInverse) return false
    if (a.isConceal != other.isConceal) return false
    if (a.isStrikeThrough != other.isStrikeThrough) return false
    if (a.isHidden != other.isHidden) return false
    if (a.hasForeground != other.hasForeground) return false
    if (a.hasBackground != other.hasBackground) return false
    if (a.foreground != other.foreground) return false
    if (a.background != other.background) return false

    return true
}

fun Flavor.hash(): Int {
    var result = isBold.hashCode()
    result = 31 * result + isFaint.hashCode()
    result = 31 * result + isItalic.hashCode()
    result = 31 * result + isUnderline.hashCode()
    result = 31 * result + isBlink.hashCode()
    result = 31 * result + isInverse.hashCode()
    result = 31 * result + isConceal.hashCode()
    result = 31 * result + isStrikeThrough.hashCode()
    result = 31 * result + isHidden.hashCode()
    result = 31 * result + hasForeground.hashCode()
    result = 31 * result + hasBackground.hashCode()
    result = 31 * result + foreground.hashCode()
    result = 31 * result + background.hashCode()
    return result
}

