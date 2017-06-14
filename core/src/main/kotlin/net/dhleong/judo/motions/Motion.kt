package net.dhleong.judo.motions

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.input.InputBuffer
import java.util.EnumSet
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

typealias MotionCalculator =
    (readKey: () -> KeyStroke, buffer: CharSequence, cursor: Int) -> IntRange

interface Motion {
    enum class Flags {
        INCLUSIVE,
        TEXT_OBJECT
    }

    val flags: EnumSet<Flags>

    val isInclusive: Boolean
        get() = flags.contains(Motion.Flags.INCLUSIVE)

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

    /** NOTE: Use ONLY when ABSOLUTELY SURE the motion won't need readKey */
    fun calculate(input: CharSequence, cursor: Int) =
        calculate({ throw IllegalStateException("Expected to not need readKey") }, input, cursor)
}


internal fun createMotion(
        flag: Motion.Flags,
        calculate: (buffer: CharSequence, cursor: Int) -> IntRange): Motion =
    createMotion(listOf(flag), calculate)
internal fun createMotion(
        flags: List<Motion.Flags> = emptyList(),
        calculate: (buffer: CharSequence, cursor: Int) -> IntRange): Motion =
    createMotion(flags) { _, buffer, cursor ->
        calculate(buffer, cursor)
    }
internal fun createMotion(
        flag: Motion.Flags,
        calculate: MotionCalculator): Motion =
    createMotion(listOf(flag), calculate)
internal fun createMotion(
    flags: List<Motion.Flags> = emptyList(),
    calculate: MotionCalculator): Motion {

    return object : Motion {
        override val flags: EnumSet<Motion.Flags> = EnumSet(flags)
        override fun calculate(readKey: () -> KeyStroke, buffer: CharSequence, cursor: Int): IntRange =
            calculate(readKey, buffer, cursor)
    }
}

inline fun <reified T : Enum<T>> EnumSet(items: List<T>): EnumSet<T> =
    when (items.size) {
        0 -> EnumSet.noneOf(T::class.java)
        1 -> EnumSet.of(items[0])
        2 -> EnumSet.of(items[0], items[1])
        3 -> EnumSet.of(items[0], items[1], items[2])
        else -> EnumSet.of(items[0], *items.drop(1).toTypedArray())
    }
