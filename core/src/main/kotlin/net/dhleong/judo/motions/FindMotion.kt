package net.dhleong.judo.motions

import net.dhleong.judo.StateKind

/**
 * @author dhleong
 */

typealias FindCalculator = (Int, Char, CharSequence, Int) -> IntRange
data class FindInfo(
    val calculate: FindCalculator,
    val step: Int,
    val target: Char,
    val offset: Int = 0
)

val KEY_LAST_FIND = StateKind<FindInfo>("net.dhleong.judo.motions.lastFind")


fun calculateFind(step: Int, target: Char, buffer: CharSequence, start: Int): IntRange {
    val end: Int
    if (step > 0) {
        end = buffer.indexOf(target, start + 1)
    } else {
        end = buffer.lastIndexOf(target, start - 1)
    }

    return if (end == -1) {
        start..start
    } else {
        start..end
    }
}

fun findMotion(step: Int): Motion {
    val flags =
        if (step > 0) listOf(Motion.Flags.INCLUSIVE)
        else emptyList()
    return createMotion(flags) { core, buffer, start ->
        val target = core.readKey().keyChar
        core.state[KEY_LAST_FIND] = FindInfo(::calculateFind, step, target)
        calculateFind(step, target, buffer, start)
    }
}


fun calculateTil(step: Int, target: Char, buffer: CharSequence, start: Int): IntRange {
    val baseRange = calculateFind(step, target, buffer, start)
    return baseRange.start..(baseRange.endInclusive - step)
}

fun tilMotion(step: Int): Motion {
    val flags =
        if (step > 0) listOf(Motion.Flags.INCLUSIVE)
        else emptyList()
    return createMotion(flags) { core, buffer, cursor ->
        val target = core.readKey().keyChar
        core.state[KEY_LAST_FIND] = FindInfo(::calculateTil, step, target, offset = step)
        calculateTil(step, target, buffer, cursor)
    }
}


fun repeatFindMotion(step: Int): Motion =
    createMotion { core, buffer, cursor ->
        val data = core.state[KEY_LAST_FIND]
        data?.calculate?.invoke(
            step * data.step,
            data.target,
            buffer,
            cursor + data.offset
        ) ?: cursor..cursor
    }
