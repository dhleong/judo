package net.dhleong.judo.jline

import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.flavor.Flavor
import net.dhleong.judo.theme.ColorTheme
import net.dhleong.judo.theme.transformBackground
import net.dhleong.judo.theme.transformForeground
import org.jline.utils.AttributedStyle
import kotlin.math.round

// from AttributedStyle source, since they're not public...
private const val F_BOLD = 0x00000001
private const val F_FAINT = 0x00000002
private const val F_ITALIC = 0x00000004
private const val F_UNDERLINE = 0x00000008
private const val F_BLINK = 0x00000010
private const val F_INVERSE = 0x00000020
private const val F_CONCEAL = 0x00000040
private const val F_CROSSED_OUT = 0x00000080
private const val F_FOREGROUND = 0x00000100
private const val F_BACKGROUND = 0x00000200
private const val F_HIDDEN = 0x00000400

fun Flavor.toAttributedStyle(
    colorTheme: ColorTheme? = null
): AttributedStyle = createAttributedStyle(
    0 or isBold.toFlag(F_BOLD)
        or isFaint.toFlag(F_FAINT)
        or isItalic.toFlag(F_ITALIC)
        or isUnderline.toFlag(F_UNDERLINE)
        or isBlink.toFlag(F_BLINK)
        or isInverse.toFlag(F_INVERSE)
        or isConceal.toFlag(F_CONCEAL)
        or isStrikeThrough.toFlag(F_CROSSED_OUT)
        or hasForeground.toFlag(F_FOREGROUND)
        or hasBackground.toFlag(F_BACKGROUND)
        or isHidden.toFlag(F_HIDDEN)
).let { base ->

    // TODO do this without these extra allocations?
    var style = base
    if (hasForeground) {
        val fg = colorTheme.transformForeground(foreground)
        style = if (fg is JudoColor.Default) {
            style.foregroundDefault()
        } else {
            style.foreground(fg.toAnsiInt())
        }
    }
    if (hasBackground) {
        val bg = colorTheme.transformBackground(background)
        style = if (bg is JudoColor.Default) {
            style.backgroundDefault()
        } else {
            style.background(bg.toAnsiInt())
        }
    }

    style
}

private val styleFactory by lazy(LazyThreadSafetyMode.NONE) {
    val constructor = AttributedStyle::class.java.getDeclaredConstructor(
        Int::class.java,
        Int::class.java
    ).apply {
        isAccessible = true
    }
    return@lazy { style: Int, mask: Int ->
        constructor.newInstance(style, mask)
    }
}

fun createAttributedStyle(styleInt: Int): AttributedStyle = styleFactory(
    styleInt,
    styleInt
)

@Suppress("NOTHING_TO_INLINE")
private inline fun Boolean.toFlag(flagValue: Int): Int =
    if (this) flagValue
    else 0

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
