package net.dhleong.judo.jline

import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.flavor.Flavor
import net.dhleong.judo.theme.ColorTheme
import net.dhleong.judo.theme.transformBackground
import net.dhleong.judo.theme.transformForeground
import org.jline.utils.AttributedStyle
import kotlin.math.round

// from AttributedStyle source, since they're not public...
private const val F_BOLD = 0x00000001L
private const val F_FAINT = 0x00000002L
private const val F_ITALIC = 0x00000004L
private const val F_UNDERLINE = 0x00000008L
private const val F_BLINK = 0x00000010L
private const val F_INVERSE = 0x00000020L
private const val F_CONCEAL = 0x00000040L
private const val F_CROSSED_OUT = 0x00000080L
private const val F_FOREGROUND = 0x00000100L
private const val F_BACKGROUND = 0x00000400L
private const val F_HIDDEN = 0x00001000L

fun Flavor.toAttributedStyle(
    colorTheme: ColorTheme? = null
): AttributedStyle = createAttributedStyle(
    0L or isBold.toFlag(F_BOLD)
        or isFaint.toFlag(F_FAINT)
        or isItalic.toFlag(F_ITALIC)
        or isUnderline.toFlag(F_UNDERLINE)
        or isBlink.toFlag(F_BLINK)
        or isInverse.toFlag(F_INVERSE)
        or isConceal.toFlag(F_CONCEAL)
        or isStrikeThrough.toFlag(F_CROSSED_OUT)
//        or hasForeground.toFlag(F_FOREGROUND)
//        or hasBackground.toFlag(F_BACKGROUND)
        or isHidden.toFlag(F_HIDDEN)
).let { base ->

    // TODO do this without these extra allocations?
    var style = base
    if (hasForeground) {
        val fg = colorTheme.transformForeground(foreground)
        style = when (fg) {
            is JudoColor.Default -> style.foregroundDefault()
            is JudoColor.FullRGB -> style.foreground(fg.red, fg.green, fg.blue)
            else -> style.foreground(fg.toAnsiInt())
        }
    }
    if (hasBackground) {
        val bg = colorTheme.transformBackground(background)
        style = when (bg) {
            is JudoColor.Default -> style.backgroundDefault()
            is JudoColor.FullRGB -> style.background(bg.red, bg.green, bg.blue)
            else -> style.background(bg.toAnsiInt())
        }
    }

    style
}

fun createAttributedStyle(styleLong: Long): AttributedStyle = AttributedStyle(
    styleLong,
    styleLong
)

@Suppress("NOTHING_TO_INLINE")
private inline fun Boolean.toFlag(flagValue: Long): Long =
    if (this) flagValue
    else 0L

internal fun JudoColor.toAnsiInt(): Int = when (this) {
    is JudoColor.Simple -> value.ansi
    is JudoColor.High256 -> value
    is JudoColor.FullRGB -> {
        // round to 256 colors
        roundAnsiRgb(red, green, blue)
    }
    JudoColor.Default -> throw IllegalArgumentException("Don't use toAnsiInt with JudoColor.Default")
}

// from https://github.com/chalk/ansi-styles/issues/11
@Suppress("NOTHING_TO_INLINE")
private inline fun roundAnsiRgb(r8: Int, g8: Int, b8: Int): Int {
    val r = applyDomain(r8, 0, 255, 0, 5)
    val g = applyDomain(g8, 0, 255, 0, 5)
    val b = applyDomain(b8, 0, 255, 0, 5)
    return ((36 * r) + (6 * g) + b + 16).toInt()
}
@Suppress("NOTHING_TO_INLINE", "SameParameterValue")
private inline fun applyDomain(v: Int, lb: Int, ub: Int, tlb: Int, tub: Int): Double =
    round(((v - lb) / (ub - lb).toDouble()) * (tub - tlb) + tlb)
