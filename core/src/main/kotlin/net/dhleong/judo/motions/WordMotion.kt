package net.dhleong.judo.motions

/**
 * @author dhleong
 */

internal fun wordBoundaryFor(bigWord: Boolean): (Char) -> Boolean =
    if (bigWord) Character::isWhitespace
    else { char -> !Character.isJavaIdentifierPart(char) }

fun wordMotion(step: Int, bigWord: Boolean): Motion {
    val isWordBoundary = wordBoundaryFor(bigWord)
    return createMotion { buffer, start ->
        var end = start

        var wasOnBoundary = end >= buffer.length || isWordBoundary(buffer[end])

        end += step

        if (step < 0) {
            // skip past any whitespace
            while (end > 0 && Character.isWhitespace(buffer[end])) {
                end += step
            }

            wasOnBoundary = end >= 0 && isWordBoundary(buffer[end])
        } else if (end >= buffer.length) {
            // we've gone too far this time
            end = buffer.length - 1
        }

        if (!(wasOnBoundary && end < buffer.length && end >= 0 && !isWordBoundary(buffer[end]))) {
            // find the next word boundary
            while (end in 0 until buffer.length && !isWordBoundary(buffer[end])) {
                end += step
            }
        }

        if (step > 0) {
            // keep going to start of next word
            while (end in 0 until (buffer.length - 1) && Character.isWhitespace(buffer[end])) {
                end += step
            }

        } else if (!wasOnBoundary && (end < 0 || isWordBoundary(buffer[end]))) {
            // shift back to the start of the previous word
            ++end
        }

        start..end
    }
}

fun endOfWordMotion(step: Int, bigWord: Boolean): Motion {
    val isWordBoundary = wordBoundaryFor(bigWord)
    return createMotion(Motion.Flags.INCLUSIVE) { buffer, start ->
        var end = start

        end += step

        var wasOnBoundary = end <= 0 || end >= buffer.length || isWordBoundary(buffer[end])

        if (!wasOnBoundary && step < 0) {
            // skip past the current word
            while (end in 0..buffer.lastIndex && !isWordBoundary(buffer[end])) {
                end += step
            }
        }

        // skip past any whitespace
        while (end in 0..buffer.lastIndex && Character.isWhitespace(buffer[end])) {
            end += step
        }

        if (step > 0) {
            wasOnBoundary = end in 0..buffer.lastIndex && isWordBoundary(buffer[end])
        }

        if (!wasOnBoundary && step > 0) {
            // continue to the end
            while (end in 0..buffer.lastIndex && !isWordBoundary(buffer[end])) {
                end += step
            }

            // pop back
            end -= step
        }

        start..end
    }
}
