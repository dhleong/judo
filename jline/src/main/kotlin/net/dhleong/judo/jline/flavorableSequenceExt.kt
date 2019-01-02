package net.dhleong.judo.jline

import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.forEachChunk
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.WCWidth

fun FlavorableCharSequence.toAttributedString(
): AttributedString = AttributedStringBuilder(length).also { result ->
    forEachChunk { startIndex, endIndex, flavor ->
        result.append(subSequence(startIndex, endIndex), flavor.toAttributedStyle())
    }
}.toAttributedString()

fun FlavorableCharSequence.computeRenderedLinesCount(
    windowWidth: Int,
    wordWrap: Boolean
): Int {
    var lines = 0
    forEachRenderedLine(windowWidth, wordWrap) { _, _ ->
        ++lines
    }
    return lines
}

internal inline fun FlavorableCharSequence.forEachRenderedLine(
    windowWidth: Int,
    wordWrap: Boolean,
    block: (start: Int, end: Int) -> Unit
) {
    val len = length
    if (len == 0) {
        block(0, 0)
        return
    }

    var lineStart = 0
    var i = 0
    var currentWidth = 0
    var lastWordEnd = 0
    var lastWordStart = 0
    var wasInWord = false

    while (i < len) {
        var cp = codePointAt(i)
        val charWidth =
            if (isHidden(i)) 0
            else WCWidth.wcwidth(cp)

        val inWord = !Character.isWhitespace(cp)
        when {
            wasInWord && !inWord -> lastWordEnd = i

            !wasInWord && inWord -> lastWordStart = i
        }
        wasInWord = inWord

        var foundLine = true
        val newWidth = currentWidth + charWidth
        i += Character.charCount(cp)
        currentWidth = when {
            // still room
            newWidth < windowWidth -> {
                foundLine = false
                newWidth
            }

            // word wrap right on a boundary
            wordWrap && inWord && (i == len || Character.isWhitespace(codePointAt(i))) -> {
                block(lineStart, i)
                lineStart = i

                // no width since we're on a boundary
                0
            }

            // new line with word wrap
            wordWrap && lastWordEnd > lineStart -> {
                block(lineStart, lastWordEnd)
                if (lastWordStart == lineStart) {
                    lineStart = i

                    newWidth - windowWidth
                } else {
                    lineStart = lastWordStart

                    // compute the width since there could be some
                    // whitespace
                    var w = 0
                    for (j in lineStart until i) {
                        w += if (isHidden(j)) 0
                            else WCWidth.wcwidth(codePointAt(j))
                    }
                    w
                }
            }

            // simple-split newline (or word wrap without a word boundary)
            else -> {
                block(lineStart, i)
                lineStart = i
                lastWordStart = i

                newWidth - windowWidth
            }
        }

        // if we word-wrapped on a boundary, try to skip any whitespace
        if (foundLine && wordWrap && lineStart < len) {
            cp = codePointAt(lineStart)
            while (lineStart < len && Character.isWhitespace(cp)) {
                val charCount = Character.charCount(cp)
                lineStart += charCount
                i += charCount
                cp = codePointAt(lineStart)
            }
        }
    }

    if (currentWidth > 0) {
        block(lineStart, i)
    }
}

