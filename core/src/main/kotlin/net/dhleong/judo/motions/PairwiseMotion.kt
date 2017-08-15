package net.dhleong.judo.motions

/**
 * @author dhleong
 */

fun innerPairwiseMotion(open: Char, close: Char): Motion =
    createMotion(Motion.Flags.TEXT_OBJECT) { buffer, cursor ->
        calculateInnerPair(open, close, buffer, cursor)
    }

fun outerPairwiseMotion(open: Char, close: Char): Motion =
    createMotion(Motion.Flags.TEXT_OBJECT) { buffer, cursor ->
        val inner = calculateInnerPair(open, close, buffer, cursor)
        if (inner.first == inner.last) inner // no dice
        else {
            inner.first - 1..inner.last + 1
        }
    }

internal fun calculateInnerPair(
    open: Char, close: Char,
    buffer: CharSequence, cursor: Int,
    isRecursion: Boolean = false
): IntRange {
    val closeRange = calculateFind(1, close, buffer, cursor)
    val closeEnd = closeRange.last
    if (closeEnd == cursor &&
            // NOTE: if closeEnd is within the buffer, it could actually
            // be the closing pair!
            (closeEnd >= buffer.length || buffer[closeEnd] != close)) {
        // couldn't find close? just give up
        return closeRange
    }

    val openRange = calculateFind(-1, open, buffer, closeRange.last)
    val foundOpen = openRange.last < closeRange.last
    if (!foundOpen && (open != close || isRecursion)) {
        // found close, but not open; go nowhere
        return cursor..cursor
    } else if (!foundOpen && open == close) {
        // didn't find open, but we're in a double pair, like quotes,
        // and haven't recursed yet. So, we get one more shot
        val fromRecursion = calculateInnerPair(
            open, close,
            buffer, closeRange.last + 1,
            isRecursion = true)
        if (fromRecursion.first == fromRecursion.last) {
            // nothing found; don't move anywhere
            return cursor..cursor
        } else {
            // huzzah! return what we found
            return fromRecursion
        }
    } else {
        // found it!
        return openRange.last + 1..closeRange.last
    }
}