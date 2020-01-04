package net.dhleong.judo.theme

import net.dhleong.judo.StateKind
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.flavor.flavor

typealias ColorTransform = (JudoColor) -> JudoColor

sealed class UiElement {
    object Dividers : UiElement()
    object Editor : UiElement()
    object Echo : UiElement()

    companion object {
        fun fromString(s: String): UiElement? = when (s) {
            "dividers" -> Dividers
            "editor" -> Editor
            "echo" -> Echo

            else -> null
        }
    }
}

/**
 * @author dhleong
 */
data class ColorSet(
    val default: JudoColor? = null,
    val simple: Map<JudoColor, JudoColor?>? = null,
    val transform256: ((Int) -> JudoColor)? = null,
    val transformRgb: ((Int, Int, Int) -> JudoColor)? = null
) : ColorTransform {

    override fun invoke(original: JudoColor): JudoColor = when (original) {
        is JudoColor.Default -> default
        is JudoColor.Simple -> simple?.get(original)
        is JudoColor.High256 -> transform256?.invoke(original.value)
        is JudoColor.FullRGB -> transformRgb?.invoke(original.red, original.green, original.blue)
    } ?: original

}

data class ColorTheme(
    val foreground: ColorTransform? = null,
    val background: ColorTransform? = null
)

data class UiTheme(
    val elements: Map<UiElement, JudoColor?>? = null
)

data class AppColors(
    val output: ColorTheme = ColorTheme(),
    val foreground: UiTheme? = null,
    val background: UiTheme? = null
) {
    companion object Key : StateKind<AppColors>("net.dhleong.judo.colorTheme")
}

fun ColorTheme?.transformForeground(color: JudoColor) =
    this?.foreground?.invoke(color) ?: color

fun ColorTheme?.transformBackground(color: JudoColor) =
    this?.background?.invoke(color) ?: color

val AppColors?.fg: UiTheme? get() = this?.foreground
val AppColors?.bg: UiTheme? get() = this?.background

operator fun UiTheme?.get(element: UiElement) =
    this?.elements?.get(element) ?: JudoColor.Default

operator fun AppColors?.get(element: UiElement) = flavor(
    foreground = fg[element].takeUnless { it is JudoColor.Default },
    background = bg[element].takeUnless { it is JudoColor.Default }
)

