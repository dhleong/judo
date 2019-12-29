package net.dhleong.judo.render.flavor

import net.dhleong.judo.render.JudoColor

private const val F_BOLD = 0x00000001
private const val F_FAINT = 0x00000002
private const val F_ITALIC = 0x00000004
private const val F_UNDERLINE = 0x00000008
private const val F_BLINK = 0x00000010
private const val F_INVERSE = 0x00000020
private const val F_CONCEAL = 0x00000040
private const val F_STRIKETHROUGH = 0x00000080
private const val F_FOREGROUND = 0x00000100
private const val F_BACKGROUND = 0x00000200
private const val F_HIDDEN = 0x00000400

private const val MASK_FOREGROUND = 0x00FF0000
private const val MASK_BACKGROUND = 0xFF000000.toInt()
private const val MASK_FOREGROUND_SHIFT = 16
private const val MASK_BACKGROUND_SHIFT = 24

/**
 * IntFlavor is a more memory-efficient implementation of [Flavor]
 * for the common case of simple colors and styles. It does not
 * support RGB colors or other attachments
 *
 * @author dhleong
 */
inline class IntFlavor(
    private val value: Int = 0
) : Flavor {

    override val isBold: Boolean get() = hasFlag(F_BOLD)
    override val isFaint: Boolean get() = hasFlag(F_FAINT)
    override val isItalic: Boolean get() = hasFlag(F_ITALIC)
    override val isUnderline: Boolean get() = hasFlag(F_UNDERLINE)
    override val isBlink: Boolean get() = hasFlag(F_BLINK)
    override val isInverse: Boolean get() = hasFlag(F_INVERSE)
    override val isConceal: Boolean get() = hasFlag(F_CONCEAL)
    override val isStrikeThrough: Boolean get() = hasFlag(F_STRIKETHROUGH)
    override val isHidden: Boolean get() = hasFlag(F_HIDDEN)
    override val hasForeground: Boolean get() = hasFlag(F_FOREGROUND)
    override val hasBackground: Boolean get() = hasFlag(F_BACKGROUND)

    override val foreground: JudoColor
        get() = if (!hasForeground) JudoColor.Default
            else maskedColor(MASK_FOREGROUND, MASK_FOREGROUND_SHIFT)
    override val background: JudoColor
        get() = if (!hasBackground) JudoColor.Default
            else maskedColor(MASK_BACKGROUND, MASK_BACKGROUND_SHIFT)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun hasFlag(flag: Int) =
        (value and flag) != 0

    @Suppress("NOTHING_TO_INLINE")
    private inline fun maskedColor(mask: Int, bitsToShift: Int) =
        (((value and mask) shr bitsToShift) and 0xFF).let { number ->
            if (number < 16) JudoColor.Simple.from(number)
            else JudoColor.High256(number)
        }
}

@Suppress("FunctionName")
fun IntFlavor(
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
): IntFlavor = IntFlavor(
    0 or
        F_BOLD.flagIf(isBold) or
        F_FAINT.flagIf(isFaint) or
        F_ITALIC.flagIf(isItalic) or
        F_UNDERLINE.flagIf(isUnderline) or
        F_BLINK.flagIf(isBlink) or
        F_INVERSE.flagIf(isInverse) or
        F_CONCEAL.flagIf(isConceal) or
        F_STRIKETHROUGH.flagIf(isStrikeThrough) or
        F_HIDDEN.flagIf(isHidden) or
        F_FOREGROUND.flagIf(hasForeground) or
        F_BACKGROUND.flagIf(hasBackground) or
        foreground.mask(MASK_FOREGROUND, MASK_FOREGROUND_SHIFT) or
        background.mask(MASK_BACKGROUND, MASK_BACKGROUND_SHIFT)
)

@Suppress("FunctionName")
fun IntFlavor(other: Flavor): IntFlavor = IntFlavor(
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

private fun Int.flagIf(condition: Boolean) =
    if (condition) this
    else 0

private fun JudoColor.mask(mask: Int, bitsToShift: Int): Int {
    val intVal = when (val c = this) {
        is JudoColor.Default -> 0
        is JudoColor.Simple -> c.value.ansi
        is JudoColor.High256 -> c.value
        else -> throw IllegalArgumentException("IntFlavor does not support $c")
    }
    return ((intVal.toLong() shl bitsToShift) and mask.toLong()).toInt()
}
