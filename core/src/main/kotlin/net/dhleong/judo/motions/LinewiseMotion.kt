package net.dhleong.judo.motions

import net.dhleong.judo.modes.output.OutputBufferCharSequence

/**
 * @author dhleong
 */
fun verticalLineMotion(step: Int): Motion = createLinewiseMotion(
    Motion.Flags.INCLUSIVE
) { buffer, cursor ->
    var end = (cursor + step * OutputBufferCharSequence.CHARS_PER_LINE)
    if (end < 0 || end >= buffer.length) {
        // out of bounds; don't move
        return@createLinewiseMotion cursor..cursor
    }

    while (end > 0 && buffer[end].isWhitespace()) {
        --end
    }

    cursor..end
}
