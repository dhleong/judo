package net.dhleong.judo.motions

import net.dhleong.judo.input.InputBuffer

/**
 * @author dhleong
 */

interface Motion {
    fun applyTo(buffer: InputBuffer) {
        buffer.cursor = calculate(buffer.toChars(), buffer.cursor).endInclusive
    }

    fun calculate(buffer: CharSequence, cursor: Int): IntRange
}

internal fun createMotion(calculate: (buffer: CharSequence, cursor: Int) -> IntRange): Motion {
    return object : Motion {
        override fun calculate(buffer: CharSequence, cursor: Int): IntRange =
            calculate(buffer, cursor)
    }
}
