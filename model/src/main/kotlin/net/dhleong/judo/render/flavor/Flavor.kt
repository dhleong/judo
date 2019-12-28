package net.dhleong.judo.render.flavor

import net.dhleong.judo.render.JudoColor

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
): Flavor = SimpleFlavor(
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

fun flavor(old: Flavor): Flavor = SimpleFlavor(old)
