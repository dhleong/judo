package net.dhleong.judo.motions

import net.dhleong.judo.DUMMY_JUDO_CORE
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.input.InputBuffer
import java.util.EnumSet

/**
 * @author dhleong
 */

typealias MotionCalculator =
    suspend (core: IJudoCore, buffer: CharSequence, cursor: Int) -> IntRange

interface Motion {
    enum class Flags {
        INCLUSIVE,
        TEXT_OBJECT
    }

    val flags: EnumSet<Flags>

    val isInclusive: Boolean
        get() = flags.contains(Flags.INCLUSIVE)

    val isTextObject: Boolean
        get() = flags.contains(Flags.TEXT_OBJECT)

    suspend fun applyTo(core: IJudoCore, buffer: InputBuffer) {
        val end = calculate(
            core, buffer.toChars(), buffer.cursor
        ).last

        buffer.cursor = minOf(buffer.size, maxOf(0, end))
    }

    suspend fun calculate(core: IJudoCore, buffer: InputBuffer) =
        calculate(core, buffer.toChars(), buffer.cursor)

    suspend fun calculate(core: IJudoCore, buffer: CharSequence, cursor: Int): IntRange

    /** NOTE: Use ONLY when ABSOLUTELY SURE the motion won't need readKey */
    suspend fun calculate(input: CharSequence, cursor: Int) =
        calculate(DUMMY_JUDO_CORE, input, cursor)

    /**
     * Can be overridden to provide an alternative motion when being repeated
     */
    fun toRepeatable(): Motion = this
}

fun IntRange.normalizeForMotion(motion: Motion): IntRange =
    if (motion.isInclusive && first <= last) {
        first..(last + 1)
    } else if (motion.isInclusive && first > last) {
        first..(last + 1)
    } else {
        this
    }

internal fun createMotion(
    flag: Motion.Flags,
    calculate: suspend (buffer: CharSequence, cursor: Int) -> IntRange
): Motion = createMotion(listOf(flag), calculate)
internal fun createMotion(
    flags: List<Motion.Flags> = emptyList(),
    calculate: suspend (buffer: CharSequence, cursor: Int) -> IntRange
): Motion = createMotion(flags) { _, buffer, cursor ->
    calculate(buffer, cursor)
}
internal fun createMotion(
    flags: List<Motion.Flags> = emptyList(),
    calculate: MotionCalculator
): Motion = object : Motion {
    override val flags: EnumSet<Motion.Flags> = EnumSet(flags)
    override suspend fun calculate(core: IJudoCore, buffer: CharSequence, cursor: Int): IntRange =
        calculate(core, buffer, cursor)
}

internal infix fun Motion.repeatWith(repeatable: Motion): Motion {
    val original = this
    return object : Motion {
        override val flags = original.flags
        override suspend fun calculate(core: IJudoCore, buffer: CharSequence, cursor: Int): IntRange =
            original.calculate(core, buffer, cursor)

        override fun toRepeatable(): Motion = repeatable
    }
}

@Suppress("FunctionName")
inline fun <reified T : Enum<T>> EnumSet(items: List<T>): EnumSet<T> =
    when (items.size) {
        0 -> EnumSet.noneOf(T::class.java)
        1 -> EnumSet.of(items[0])
        2 -> EnumSet.of(items[0], items[1])
        3 -> EnumSet.of(items[0], items[1], items[2])
        else -> EnumSet.of(items[0], *items.drop(1).toTypedArray())
    }
