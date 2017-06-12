package net.dhleong.judo.util

import java.util.ConcurrentModificationException

/**
 * @author dhleong
 */

/**
 * Create a "forgiving" sequence which iterates over items from the
 * iterableSource.  Iterating over such a Sequence shields you from
 * [java.util.ConcurrentModificationException] by grabbing a new
 * Iterable from [iterableSource] and attempting to skip over items
 * that had already been iterated past.
 *
 * Note: this is best effort only, and some items may be lost, especially
 * if items were inserted at the beginning of the sequence.
 *
 * @param iterableSource Factory for Iterable instances that generally
 *  return the same sequence
 */
fun <T : Any> ForgivingSequence(iterableSource: () -> Iterable<T>): Sequence<T> {
    var currentIterator = iterableSource.invoke().iterator()
    var iterated = 0
    return generateSequence {
        var result: T?
        try {
            result = currentIterator.nextOrNull()
            ++iterated
        } catch (e: ConcurrentModificationException) {
            currentIterator = iterableSource.invoke().iterator()
            result = currentIterator.skipAndNext(iterated)
        }

        result
    }
}

private fun <T> Iterator<T>.skipAndNext(skip: Int): T? {
    for (i in 0 until skip) {
        if (nextOrNull() == null) return null
    }

    return nextOrNull()
}

private fun <T> Iterator<T>.nextOrNull() =
    if (!hasNext()) null
    else next()
