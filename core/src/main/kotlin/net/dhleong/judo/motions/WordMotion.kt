package net.dhleong.judo.motions

/**
 * @author dhleong
 */

fun wordMotion(step: Int, bigWord: Boolean): Motion {
    val isWordBoundary: (Char) -> Boolean =
        if (bigWord) Character::isWhitespace
        else { char -> !Character.isJavaIdentifierPart(char) }
    return createMotion { buffer, start ->
        var end = start

        var wasOnBoundary = end >= buffer.length || isWordBoundary(buffer[end])

        end += step

        if (step < 0) {
            // skip past any whitespace
            while (end > 0 && Character.isWhitespace(buffer[end])) {
                end += step
            }

            wasOnBoundary = isWordBoundary(buffer[end])
        }

        if (!(wasOnBoundary && !isWordBoundary(buffer[end]))) {
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
