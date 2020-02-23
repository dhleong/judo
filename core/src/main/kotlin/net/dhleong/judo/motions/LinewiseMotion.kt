package net.dhleong.judo.motions

import net.dhleong.judo.modes.output.OutputBufferCharSequence

/**
 * @author dhleong
 */
fun verticalLineMotion(step: Int): Motion = createLinewiseMotion(
    Motion.Flags.INCLUSIVE
) { buffer, cursor ->
    var end = (cursor + step * OutputBufferCharSequence.CHARS_PER_LINE).coerceAtLeast(0)
    while (end > 0 && buffer[end].isWhitespace()) {
        --end
    }

    cursor..end
}
