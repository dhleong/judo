package net.dhleong.judo.jline

import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.forEachChunk
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.WCWidth

fun FlavorableCharSequence.toAttributedString(
    widthForTrailingFlavor: Int = -1
): AttributedString = AttributedStringBuilder(
    // NOTE JLine 3.5.1 has an error where appending to a 0-capacity
    // Builder will hang forever; we also may need room to handle
    // trailing flavor anyway, so... add 1
    length + 1
).also { result ->
    appendTo(result)

    // if there's no room for the trailing flavor... it doesn't matter,
    // because it wouldn't be seen anyway
    if (
        widthForTrailingFlavor > 0
        && result.length < widthForTrailingFlavor
    ) {
        trailingFlavor?.let { flavor ->
            result.append(" ", flavor.toAttributedStyle())
        }
    }
}.toAttributedString()

fun FlavorableCharSequence.appendTo(
    builder: AttributedStringBuilder
) {
    forEachChunk { startIndex, endIndex, flavor ->
        builder.append(subSequence(startIndex, endIndex), flavor.toAttributedStyle())
    }
}


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

/**
 * Returns the index of the chunk in which [offset] would
 * appear if this sequence were split according to [windowWidth]
 * and [wordWrap]
 */
fun FlavorableCharSequence.splitIndexOfOffset(
    windowWidth: Int,
    wordWrap: Boolean,
    offset: Int
): Int {
    var index = 0
    forEachRenderedLine(windowWidth, wordWrap) { startIndex, endIndex ->
        if (offset in startIndex until endIndex) {
            return index
        }

        ++index
    }
    return -1
}

/**
 * Given an [offset] into this sequence, return the
 * offset into the chunk on which [offset] would
 * ultimately be rendered
 */
fun FlavorableCharSequence.splitOffsetOfOffset(
    windowWidth: Int,
    wordWrap: Boolean,
    offset: Int
): Int {
    forEachRenderedLine(windowWidth, wordWrap) { startIndex, endIndex ->
        if (offset in startIndex until endIndex) {
            return offset - startIndex
        }
    }
    return -1
}

internal inline fun FlavorableCharSequence.forEachRenderedLine(
    windowWidth: Int,
    wordWrap: Boolean,
    preserveWhitespace: Boolean = false,
    block: (start: Int, end: Int) -> Unit
) {
    var len = length
    if (len == 0) {
        block(0, 0)
        return
    }

    // trim off trailing newline
    if (this[len - 1] == '\n') {
        --len
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
            (
                wordWrap
                    && inWord
                    && (i == len || Character.isWhitespace(codePointAt(i)))
            ) -> {
                block(lineStart, i)
                lineStart = i

                // no width since we're on a boundary
                0
            }

            // new line with word wrap
            wordWrap && lastWordEnd > lineStart -> {
                if (preserveWhitespace) {
                    block(lineStart, lastWordStart)
                } else {
                    block(lineStart, lastWordEnd)
                }

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
        if (!preserveWhitespace && foundLine && wordWrap && lineStart < len) {
            cp = codePointAt(lineStart)
            while (lineStart < len && Character.isWhitespace(cp)) {
                val charCount = Character.charCount(cp)
                lineStart += charCount
                i += charCount
                if (lineStart < len) {
                    cp = codePointAt(lineStart)
                }
            }
        }
    }

    if (currentWidth > 0) {
        block(lineStart, i)
    }
}

