package net.dhleong.judo.render.flavor

import net.dhleong.judo.render.JudoColor

/**
 * An explicit [Flavor] implementation using a data class.
 */
data class ExplicitFlavor(
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

    override fun equals(other: Any?): Boolean = areFlavorsEqual(this, other)
    override fun hashCode(): Int = hash()

    fun optimize() = (this as Flavor).copy()

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