package net.dhleong.judo.render

import net.dhleong.judo.util.ansi
import net.dhleong.judo.util.findTrailingEscape
import org.jline.utils.AttributedCharSequence
import org.jline.utils.AttributedString

private val ANSI_CLEAR = ansi(0)

/**
 * One logical line of output to be rendered.
 * @author dhleong
 */
class OutputLine : CharSequence {
    override val length: Int
        get() = rawChars.length

    private val rawChars: StringBuilder
    private val cachedDisplayLines = ArrayList<AttributedString>(8)

    private var dirty = true
    private var lastWindowWidth = -1

    constructor(original: CharSequence) {
        rawChars = StringBuilder(original.length)
        append(original)
    }

    constructor(buffer: CharArray, start: Int, count: Int) {
        rawChars = StringBuilder(count)
        rawChars.append(buffer, start, count)
    }

    fun append(chars: CharSequence) {
        if (chars is AttributedCharSequence) {
            val asAnsi = chars.toAnsi()
            val trailing = findTrailingEscape(asAnsi)
            if (trailing != null && trailing.endsWith(ANSI_CLEAR)) {
                rawChars.append(asAnsi.subSequence(0..asAnsi.lastIndex - ANSI_CLEAR.length))
            } else {
                rawChars.append(chars.toAnsi())
            }

            // this should no longer happen...?
//            if (chars is ReplaceableAttributedStringBuilder) {
//                chars.postEscapePartials?.let {
//                    append(it)
//                }
//            }
        } else if (chars is OutputLine) {
            rawChars.append(chars.rawChars)
        } else {
            rawChars.append(chars)
        }
        dirty = true
    }

    override fun get(index: Int): Char = rawChars[index]

    override fun subSequence(startIndex: Int, endIndex: Int): OutputLine =
        OutputLine(rawChars.subSequence(startIndex, endIndex))

    fun getDisplayedLinesCount(windowWidth: Int) =
        getDisplayLines(windowWidth).size

    fun getDisplayLines(windowWidth: Int): List<AttributedString> {
        if (!dirty && lastWindowWidth == windowWidth) return cachedDisplayLines

        cachedDisplayLines.clear()

        // TODO wrap by word?
        cachedDisplayLines.addAll(
            toAttributedString()
                .columnSplitLength(windowWidth)
        )

        dirty = false
        lastWindowWidth = windowWidth
        return cachedDisplayLines
    }

    /**
     * Split this OutputLine into enough OutputLines to render
     * comfortably within [windowWidth] columns
     */
    fun getDisplayOutputLines(windowWidth: Int): List<OutputLine> {

        if (rawChars.length < windowWidth) {
            // NOTE: this is not rigorous! Since rawChars has ansi
            // escapes in it, a very short but highly decorated line
            // will be ignored. However, this check DOES handle the
            // case of an empty line with only an ansi code on it
            // that's supposed to color the whole next paragraph
            return listOf(this)
        }

        val lines = getDisplayLines(windowWidth)
        var previousHint: CharSequence = ""
        return lines.fold(ArrayList<OutputLine>(lines.size)) { result, it ->
            val outputLine = sealedOutputLine(it, previousHint)
            result.add(outputLine)
            // FIXME we can't allow trailing here because using the JLine
            // splitter adds ansi(0) to the end no matter what :\ We need
            // to just sit down and write a word-wrap splitter that doesn't
            // force the ansi(0); it'll happen when displaying it anyway
            previousHint = outputLine.getFinalStyle(allowTrailing = false)
            result
        }
    }

    fun toAttributedString(): AttributedString {
        if (!dirty && cachedDisplayLines.size == 1) {
            return cachedDisplayLines[0]
        }

        return AttributedString.fromAnsi(rawChars.toString())
    }

    fun toAnsi(): String {
        return rawChars.toString()
    }

    override fun toString(): String = toAnsi()

    fun getFinalStyle(allowTrailing: Boolean = true): CharSequence {
        if (allowTrailing) {
            val trailing = findTrailingEscape(rawChars)
            if (trailing != null) return trailing
        }

        val attributed: AttributedCharSequence
        if (lastWindowWidth != -1) {
            val lines = getDisplayLines(lastWindowWidth)
            attributed = lines.last()
        } else {
            attributed = toAttributedString()
        }

        if (attributed.isEmpty() && rawChars.isEmpty()) {
            return ""
        } else if (attributed.isEmpty()) {
            // we have raw chars but no attributed? must be
            // ansi escape codes
            return rawChars
        }

        val style = attributed.styleAt(attributed.lastIndex)
        val stylishSpace = AttributedString(" ", style)
        return stylishSpace.toAnsi().dropLast(5) // `\e[0m  plus the space
    }

    /**
     * NOTE: Only useful once; subsequent calls are not ignored,
     * but also are probably not reflected visually
     */
    fun setStyleHint(styleHint: CharSequence) {
        if (styleHint.isEmpty()) return
        if (rawChars.startsWith(styleHint)) return

        rawChars.insert(0, styleHint)
        dirty = true
    }
}

/**
 * Create a "sealed" OutputLine. Such a line is assumed to not
 * have any split escape codes and doesn't need to store the
 * raw stream
 */
private fun sealedOutputLine(line: CharSequence, styleHint: CharSequence): OutputLine {
    // NOTE: this is an opportunity for future optimization
    // that we haven't bothered with yet
    // TODO we don't have to render AttributedString
    // into an ANSI string and copy that into the StringBuilder
    return OutputLine(line).apply {
        setStyleHint(styleHint)
    }
}
