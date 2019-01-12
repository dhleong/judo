package net.dhleong.judo.net

import net.dhleong.judo.render.Flavor
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.JudoColor

/**
 * Convert a [FlavorableCharSequence] into an ANSI string. It should
 * probably *not* be used to render to a real terminal, since not all
 * terminals will support all ANSI sequences—but it *can* be used to
 * pass to scripting environments, since it will be in a format that
 * we can parse back into a [FlavorableCharSequence] via
 * [AnsiFlavorableStringReader]
 *
 * @author dhleong
 */
fun FlavorableCharSequence.toAnsi() = StringBuilder(length + 16).also { builder ->
    var lastFlavor: Flavor? = null
    for (i in indices) {
        val flavor = getFlavor(i)
        if (flavor != lastFlavor) {
            flavor.toAnsi(lastFlavor ?: Flavor.default, builder)
            lastFlavor = flavor
        }

        builder.append(this[i])
    }

    if (lastFlavor != Flavor.default) {
        builder.append(ANSI_ESC)
        builder.append("[0m")
    }

}.toString()

private fun Flavor.toAnsi(lastFlavor: Flavor, builder: StringBuilder) {
    builder.append(ANSI_ESC)
    builder.append('[')

    if (this == Flavor.default) {
        builder.append("0m")
        return
    }

    var first = true
    first = builder.maybeAppendFlag(first, isBold, "1", lastFlavor.isBold)
    first = builder.maybeAppendFlag(first, isFaint, "2", lastFlavor.isFaint)
    first = builder.maybeAppendFlag(first, isItalic, "3", lastFlavor.isItalic)
    first = builder.maybeAppendFlag(first, isUnderline, "4", lastFlavor.isUnderline)
    first = builder.maybeAppendFlag(first, isBlink, "5", lastFlavor.isBlink)
    first = builder.maybeAppendFlag(first, isInverse, "7", lastFlavor.isInverse)
    first = builder.maybeAppendFlag(first, isConceal, "8", lastFlavor.isConceal)
    first = builder.maybeAppendFlag(first, isStrikeThrough, "9", lastFlavor.isStrikeThrough)

    first = builder.maybeAppendColor(
        first,
        hasForeground, foreground, "3", "9",
        lastFlavor.hasForeground, lastFlavor.foreground
    )
    builder.maybeAppendColor(
        first,
        hasBackground, background, "4", "10",
        lastFlavor.hasBackground, lastFlavor.background
    )

    builder.append('m')
}

private fun StringBuilder.maybeAppendFlag(
    isFirstFlag: Boolean,
    new: Boolean,
    flagIfSet: String,
    old: Boolean
) = maybePrefixParam(isFirstFlag, new != old) {

    if (!new) {
        // 2 disables the flag
        append('2')
    }

    append(flagIfSet)
}

private fun StringBuilder.maybeAppendColor(
    isFirstFlag: Boolean,
    newHasColor: Boolean,
    newColor: JudoColor,
    baseColorId: String,
    altColorId: String,
    oldHasColor: Boolean,
    oldColor: JudoColor
) = maybePrefixParam(
    isFirstFlag,
    newHasColor != oldHasColor
        || newColor != oldColor
) {

    if (!newHasColor) {
        append(baseColorId)
        append("9") // end
        return@maybePrefixParam
    }

    when (newColor) {
        is JudoColor.Simple -> {
            if (newColor.value.ansi < JudoColor.Simple.Color.BRIGHT_BLACK.ansi) {
                append(baseColorId)
                append(newColor.value.ansi)
            } else {
                append(altColorId)
                append(newColor.value.ansi - 8)
            }
        }

        is JudoColor.High256 -> {
            append(baseColorId)
            append("8;5;")
            append(newColor.value)
        }

        is JudoColor.FullRGB -> {
            append(baseColorId)
            append("8;2;")
            append(newColor.red)
            append(";")
            append(newColor.green)
            append(";")
            append(newColor.blue)
        }
    }
}

private inline fun StringBuilder.maybePrefixParam(
    isFirstFlag: Boolean,
    shouldWriteParam: Boolean,
    block: StringBuilder.() -> Unit
): Boolean {
    if (!shouldWriteParam) return isFirstFlag

    if (!isFirstFlag) {
        append(';')
    }

    block()

    return false
}
