package net.dhleong.judo.theme

import net.dhleong.judo.render.JudoColor

typealias ColorTransform = (JudoColor) -> JudoColor

/**
 * @author dhleong
 */
data class ColorSet(
    val default: JudoColor?,
    val simple: Map<JudoColor.Simple, JudoColor>?,
    val transform256: ((Int) -> JudoColor)?,
    val transformRgb: ((Int, Int, Int) -> JudoColor)?
) : ColorTransform {

    override fun invoke(original: JudoColor): JudoColor = when (original) {
        is JudoColor.Default -> default
        is JudoColor.Simple -> simple?.get(original)
        is JudoColor.High256 -> transform256?.invoke(original.value)
        is JudoColor.FullRGB -> transformRgb?.invoke(original.red, original.green, original.blue)
    } ?: original

}

data class ColorTheme(
    val foreground: ColorTransform,
    val background: ColorTransform
)

fun ColorTheme?.transformForeground(color: JudoColor) =
    this?.foreground?.invoke(color) ?: color

fun ColorTheme?.transformBackground(color: JudoColor) =
    this?.background?.invoke(color) ?: color
