package net.dhleong.judo.net

import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.JudoColor
import net.dhleong.judo.render.flavor.ExplicitFlavor
import net.dhleong.judo.render.flavor.Flavor

private enum class AnsiState {
    OFF,

    /** read ESC char */
    READ_ESC,

    /** read \e[ */
    OPEN,
}

internal const val ANSI_ESC = 27.toChar()

/**
 * Given sequences of ANSI text (IE from the network) produces
 * [FlavorableCharSequence] instances. Newlines will be included
 * at the end of complete lines.
 *
 * @author dhleong
 */
class AnsiFlavorableStringReader {

    private var state = AnsiState.OFF
    private val partialAnsi = CharArray(64)
    private var partialLength = 0

    private var lastFlavor: Flavor = Flavor.default
    private val builder = FlavorableStringBuilder(64).also {
        it.beginFlavor(Flavor.default, 0)
    }

    fun reset() {
        state = AnsiState.OFF
        partialLength = 0
        lastFlavor = Flavor.default
        builder.reset()
    }

    fun feed(
        chars: CharArray,
        start: Int = 0,
        available: Int = chars.size
    ): Sequence<FlavorableCharSequence> = sequence {
        var buffer = when {
            partialLength > 0 -> partialAnsi
            else -> chars
        }
        var limit = when {
            partialLength > 0 -> partialLength.also {
                partialLength = 0 // reset state
            }
            else -> available
        }

        var i = when {
            partialLength > 0 -> 0
            else -> start
        }
        while (i < limit) {
            val c = buffer[i]
            when {
                state == AnsiState.OFF && c == ANSI_ESC -> {
                    state = AnsiState.READ_ESC
                }

                state == AnsiState.READ_ESC && c == '[' -> {
                    state = AnsiState.OPEN
                }

                state == AnsiState.OPEN -> {
                    if (c == 'm') {
                        lastFlavor = try {
                            ansiCharsToFlavor(lastFlavor, partialAnsi, partialLength)
                        } catch (e: IllegalStateException) {
                            throw IllegalStateException(
                                "Error parsing ANSI flavor sequence: `${String(partialAnsi, 0, partialLength)}` ($partialLength)",
                                e
                            )
                        }
                        partialLength = 0
                        state = AnsiState.OFF
                    } else if (c !in '0'..'9' && c != ';') {
                        // not an SGR code, ignore
                        state = AnsiState.OFF
                    } else {
                        partialAnsi[partialLength++] = c
                    }
                }

                else -> {
                    val lastChar = when {
                        builder.isEmpty() -> (-1).toChar()
                        else -> builder.last()
                    }
                    if (lastChar == '\r' || lastChar == '\n') {
                        // normalize to a single newline character
                        builder[builder.lastIndex] = '\n'
                        yield(builder.toFlavorableString())
                        builder.fullReset()
                    }

                    when {
                        (lastChar == '\r' && c == '\r')
                        || (lastChar == '\n' && c == '\n') -> {
                            // two newlines
                            builder.append('\n', lastFlavor)
                            yield(builder.toFlavorableString())
                            builder.fullReset()
                        }

                        (lastChar == '\r' && c == '\n')
                        || (lastChar == '\n' && c == '\r') -> {
                            // ignore crlf (and a possible weird version of it)
                        }

                        else -> {
                            // common case; append the read character
                            builder.append(c, lastFlavor)
                        }
                    }
                }
            }

            if (++i >= limit && buffer === partialAnsi) {
                buffer = chars
                limit = available
                i = start
            }
        }

        if (builder.isNotEmpty()) {
            if (builder.last() == '\r') {
                // normalize
                builder[builder.lastIndex] = '\n'
            }
            val trailing = when {
                builder.last() != '\n' -> lastFlavor
                else -> null
            }
            yield(builder.toFlavorableString(trailingFlavor = trailing))
            builder.fullReset()
        }
    }
}

private fun FlavorableStringBuilder.fullReset() {
    reset()
    setFlavor(Flavor.default, 0, 1)
}

internal fun ansiCharsToFlavor(
    last: Flavor,
    ansi: CharArray,
    length: Int
): Flavor {
    // there's probably a better way to do this...
    val flavor = ExplicitFlavor(last)

    val params = ansiParameters(ansi, length)
    while (params.hasNext()) {
        when (val p = params.next()) {
            0 -> flavor.reset()
            1 -> flavor.isBold = true
            2 -> flavor.isFaint = true
            3 -> flavor.isItalic = true
            4 -> flavor.isUnderline = true
            5 -> flavor.isBlink = true
            7 -> flavor.isInverse = true
            8 -> flavor.isConceal = true
            9 -> flavor.isStrikeThrough = true
            22 -> {
                flavor.isBold = false
                flavor.isFaint = false
            }
            23 -> flavor.isItalic = false
            24 -> flavor.isUnderline = false
            25 -> flavor.isBlink = false
            27 -> flavor.isInverse = false
            28 -> flavor.isConceal = false
            29 -> flavor.isStrikeThrough = false

            in 30..37 -> {
                flavor.hasForeground = true
                flavor.foreground = JudoColor.Simple.from(p - 30)
            }
            39 -> flavor.hasForeground = false

            in 40..47 -> {
                flavor.hasBackground = true
                flavor.background = JudoColor.Simple.from(p - 40)
            }
            49 -> flavor.hasBackground = false

            38 -> {
                val color = params.readHighColor() ?: return flavor
                flavor.hasForeground = true
                flavor.foreground = color
            }
            48 -> {
                val color = params.readHighColor() ?: return flavor
                flavor.hasBackground = true
                flavor.background = color
            }

            in 90..97 -> {
                flavor.hasForeground = true
                flavor.foreground = JudoColor.Simple.from(p - 90 + 8)
            }
            in 100..107 -> {
                flavor.hasBackground = true
                flavor.background = JudoColor.Simple.from(p - 100 + 8)
            }
        }
    }

    return flavor.optimize()
}

private fun Iterator<Int>.readHighColor(): JudoColor? {
    if (!hasNext()) return null

    val kind = next()
    when (kind) {
        2 -> {
            // 24-bit rgb color
            val r = next256()
            val g = next256()
            val b = next256()
            return JudoColor.FullRGB(
                r, g, b
            )
        }

        5 -> {
            // 256 colors
            if (hasNext()) {
                return when (val color = next256()) {
                    in 0..15 -> JudoColor.Simple.from(color)
                    else -> JudoColor.High256(color)
                }
            }
        }
    }

    return null
}

private fun Iterator<Int>.next256(): Int = next().also {
    if (it !in 0..255) {
        throw IllegalStateException("Expected value in 0..255 but was $it")
    }
}

private fun ansiParameters(ansi: CharArray, length: Int) = iterator {
    var start = 0
    do {
        val end = ansi.indexOf(';', start, length).let {
            if (it == -1) length
            else it
        }

        val intValue = ansi.toInt(start, end)
        start = end + 1

        yield(intValue)
    } while (end < length)
}

private fun CharArray.indexOf(needle: Char, start: Int, end: Int): Int {
    for (i in start until end) {
        if (this[i] == needle) {
            return i
        }
    }

    return -1
}

private fun CharArray.toInt(start: Int, end: Int): Int {
    var value = 0
    for (i in start until end) {
        value *= 10
        value += this[i] - '0'
    }
    return value
}
