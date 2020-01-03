package net.dhleong.judo.script.init

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.KEY_COLOR_THEME
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.flavor.Flavor
import net.dhleong.judo.render.flavor.flavor
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.doc
import net.dhleong.judo.script.registerFn
import net.dhleong.judo.theme.ColorSet
import net.dhleong.judo.theme.ColorTheme
import net.dhleong.judo.theme.ColorTransform

enum class RecolorKind {
    FOREGROUND,
    BACKGROUND,
    ALL,
}

fun String.toKind() = when (this) {
    "FG", "fg" -> RecolorKind.FOREGROUND
    "BG", "bg" -> RecolorKind.BACKGROUND
    "ALL", "all" -> RecolorKind.ALL
    else -> throw IllegalArgumentException("Invalid recolor kind '$this'")
}

/**
 * @author dhleong
 */
fun ScriptInitContext.initColors() = with(mode) {
    registerFn<Unit>(
        "recolor",
        doc {
            usage {  }
            usage {
                arg("kind", "String")
                arg("name", "String/Int")
                arg("replacement", "String/Int/Fn")
            }
            body { """
                If invoked with no arguments, prints a demo of the current
                color theme to the current Buffer.
                
                Otherwise, update the current color theme to transform a specific color.
                    kind - For either the "fg" (foreground), "bg" (background),
                           or "all" (any time the color appears)
                    name - The color name or ansi code. The 16 basic colors
                           ("black," "bright-black," "red," "bright-red," etc)
                           may use a String or their ANSI number. The "default"
                           color must be specified by name. The extended 256-color
                           set must be specified by ANSI number. RGB colors must be
                           specified by "#RRGGBB" hex.
                           Alternatively, the following special category names may be used:
                           
                               "simple" - matches all 16 basic colors
                               "256" - matches all extended 256-color set colors
                               "rgb" - matches all RGB colors
                               
                    replacement - How to replace the matched color. If a String or Int,
                                  it is treated as a literal `name` (see above). If a
                                  function, it must be either of the form:
                                  
                                      (name: String/Int) -> String/Int
                                  
                                  or:
                                  
                                      (kind: String, name: String/Int) -> String/Int
                                  
                                  `kind` and `name` or formatted as above, but `name` will
                                  never be one of the special category names.
            """.trimIndent() }
        }
    ) { args: Array<Any> ->
        if (args.isEmpty()) {
            judo.printColors()
            return@registerFn
        }

        val (kindRaw, name, replacement) = args
        val kind = (kindRaw as String).toKind()
        recolor(kind, name, replacement)
    }

    registerFn<Unit>(
        "unrecolor",
        doc {
            usage {
                arg("kind", "String")
                arg("name", "String/Int")
            }
            body { """
                Revert a `recolor` setting; see `recolor` for arg meanings.
            """.trimIndent() }
        }
    ) { kind: String, name: Any ->
        recolor(kind.toKind(), from = name, to = null)
    }
}

internal fun IJudoCore.printColors() {
    // basic colors
    printRaw(basicColorsLine(
         toFlavor = { flavor(background = it )}
    ))
    printRaw(basicColorsLine(
        toFlavor = { flavor(foreground = it )}
    ))

    // High 256
    printRaw()
    for (i in 16 until 256 step 36) {
        printRaw(FlavorableStringBuilder(240).apply {
            for (j in i until i + 36) {
                if (j > 255) break

                append(" ", flavor(background = JudoColor.High256(j)))
            }
            trailingFlavor = Flavor.default
        })
    }

    // rgb
    val stepsPerLine = 72
    val lines = 3
    val steps = lines * stepsPerLine
    printRaw()
    for (i in 0 until steps step stepsPerLine) {
        printRaw(FlavorableStringBuilder(stepsPerLine + 1).apply {
            for (j in i until i + stepsPerLine) {
                val r = 255 - ((j * 255) / steps)
                var g = ((j * 510) / steps)
                val b = ((j * 255) / steps)
                if (g > 255) g = 510 - g

                val ch = if (j % 2 == 0) "/" else "\\"
                append(ch, flavor(
                    background = JudoColor.FullRGB(r, g, b),
                    foreground = JudoColor.FullRGB(255 - r, 255 - g, 255 - b)
                ))
            }
            trailingFlavor = Flavor.default
        })
    }

}

private inline fun basicColorsLine(
    toText: Int.() -> String = {
        if (this < 10) "___$this"
        else "__$this"
    },
    toFlavor: (JudoColor) -> Flavor
) = FlavorableStringBuilder(4 * 16).apply {
    for (i in 0 until 8) {
        append(i.toText(), toFlavor(JudoColor.Simple.from(i)))
    }

    append("  ", Flavor.default)

    for (i in 8 until 16) {
        append(i.toText(), toFlavor(JudoColor.Simple.from(i)))
    }

    trailingFlavor = Flavor.default
}

private fun ScriptInitContext.recolor(kind: RecolorKind, from: Any, to: Any?) = with(judo) {
    val old = state[KEY_COLOR_THEME] ?: ColorTheme()

    val new = with(ColorTransformer(this@recolor)) {
        when (kind) {
            RecolorKind.FOREGROUND -> old.copy(foreground = old.foreground.update(from, to))
            RecolorKind.BACKGROUND -> old.copy(background = old.background.update(from, to))
            RecolorKind.ALL -> ColorTheme(
                background = old.background.update(from, to),
                foreground = old.foreground.update(from, to)
            )
        }
    }

    state[KEY_COLOR_THEME] = new
    redraw()
}

private class ColorTransformer(private val context: ScriptInitContext) {

    fun ColorTransform?.update(from: Any, to: Any?): ColorTransform? {
        val base = (this as? ColorSet) ?: ColorSet()
        val fromSimpleColor = from.toSimpleColor()
        if (fromSimpleColor != null) {
            val old = base.simple ?: emptyMap()
            return base.copy(simple = old + mapOf(fromSimpleColor to to?.toColor()))
        }

        return when (from) {
            "simple" -> base.copy(
                simple = mutableMapOf<JudoColor.Simple, JudoColor?>().also { m ->
                    val replacement = to?.toColor()
                    for (c in JudoColor.Simple.Color.values()) {
                        m[JudoColor.Simple(c)] = replacement
                    }
                }
            )

            "256" -> base.copy(
                transform256 = to?.to256Transform()
            )

            "rgb" -> base.copy(
                transformRgb = to?.toRgbTransform()
            )

            else -> throw IllegalArgumentException("Unknown color transform from $from to $to")
        }
    }

    private fun Any.to256Transform(): (Int) -> JudoColor {
        toColor()?.let { color ->
            return { color }
        }

        val fn = context.engine.callableToFunction1(this)
        return { color ->
            val result = fn(color)
            result?.toColor()
                ?: throw IllegalArgumentException("Invalid color: $result")
        }
    }

    private fun Any.toRgbTransform(): (Int, Int, Int) -> JudoColor {
        toColor()?.let { color ->
            return { _, _, _ -> color }
        }

        val fn = context.engine.callableToFunctionN(this)
        return { red, green, blue ->
            val result = fn(arrayOf(red, green, blue))
            result?.toColor()
                ?: throw IllegalArgumentException("Invalid color: $result")
        }
    }
}

private fun Any.toSimpleColor(): JudoColor.Simple? =
    if (this !is Int || this !in 0..15) null
    else JudoColor.Simple.from(this)

private fun Any.to256Color(): JudoColor.High256? =
    if (this !is Int || this !in 16..255) null
    else JudoColor.High256(this)

private fun Any.toRgbColor(): JudoColor.FullRGB? =
    if (this !is String || !isNotEmpty() || this[0] != '#') null
    else when (this.length) {
        4 -> {
            val r = this[1]
            val g = this[2]
            val b = this[3]

            "#$r$r$g$g$b$b".toRgbColor()
        }

        7 -> try {
            val r = substring(1, 3).toInt(16)
            val g = substring(3, 5).toInt(16)
            val b = substring(5, 7).toInt(16)
            JudoColor.FullRGB(r, g, b)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid RGB color $this", e)
        }

        else -> throw IllegalArgumentException("Invalid RGB color $this")
    }


private fun Any.toColor(): JudoColor? =
    toSimpleColor()
        ?: to256Color()
        ?: toRgbColor()
