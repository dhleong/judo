package net.dhleong.judo.motions

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.input.InputBuffer
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

interface Motion {
    fun applyTo(readKey: () -> KeyStroke, buffer: InputBuffer) {
        val end = calculate(
            readKey, buffer.toChars(), buffer.cursor
        ).endInclusive

        buffer.cursor = minOf(buffer.size, maxOf(0, end))
    }

    fun calculate(readKey: () -> KeyStroke, buffer: InputBuffer) =
        calculate(readKey, buffer.toChars(), buffer.cursor)

    fun calculate(judo: IJudoCore, buffer: InputBuffer) =
        calculate(judo::readKey, buffer.toChars(), buffer.cursor)

    fun calculate(readKey: () -> KeyStroke, buffer: CharSequence, cursor: Int): IntRange
}

internal fun createMotion(calculate: (buffer: CharSequence, cursor: Int) -> IntRange): Motion =
    createMotion { _, buffer, cursor ->
        calculate(buffer, cursor)
    }
internal fun createMotion(calculate: (readKey: () -> KeyStroke, buffer: CharSequence, cursor: Int) -> IntRange): Motion {
    return object : Motion {
        override fun calculate(readKey: () -> KeyStroke, buffer: CharSequence, cursor: Int): IntRange =
            calculate(readKey, buffer, cursor)
    }
}
