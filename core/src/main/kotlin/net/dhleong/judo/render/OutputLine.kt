package net.dhleong.judo.render

import org.jline.utils.AttributedCharSequence
import org.jline.utils.AttributedString

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
            rawChars.append(chars.toAnsi())

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
        // TODO if we know we fit on a single line, we could
        // just return ourself in a singleton list

        return getDisplayLines(windowWidth)
            .map { sealedOutputLine(it) }
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
}

/**
 * Create a "sealed" OutputLine. Such a line is assumed to not
 * have any split escape codes and doesn't need to store the
 * raw stream
 */
private fun sealedOutputLine(line: CharSequence): OutputLine {
    // NOTE: this is an opportunity for future optimization
    // that we haven't bothered with yet
    // TODO we don't have to render AttributedString
    // into an ANSI string and copy that into the StringBuilder
    return OutputLine(line)
}
