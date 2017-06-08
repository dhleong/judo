package net.dhleong.judo.util

import java.util.ConcurrentModificationException

/**
 * Size-limited FIFO array-list
 * @author dhleong
 */
class CircularArrayList<E> : Collection<E> {
    private val MAX_SIZE_INCREMENT = 2048

    private val maxCapacity: Int
    private var array: Array<Any?>
    private var start: Int = 0
    private var end: Int = 0

    private var actualSize: Int = 0

    constructor(
        maxCapacity: Int,
        initialCapacity: Int = minOf(maxCapacity, 2048)
    ) {
        this.maxCapacity = maxCapacity
        array = arrayOfNulls<Any>(initialCapacity)
    }

    internal constructor(inArray: Array<Any?>, start: Int, end: Int, size: Int) {
        array = inArray
        maxCapacity = inArray.size
        this.start = start
        this.end = end
        this.actualSize = size
    }

    /**
     * @return Current capacity (but not necessarily the maxCapacity)
     */
    val capacity: Int
        get() = array.size

    val lastIndex: Int
        get() = size - 1

    override val size: Int
        get() = actualSize

    fun add(element: E) {
        ensureCapacity(actualSize + 1)
        array[end] = element

        ++end
    }

    @Suppress("UNCHECKED_CAST")
    operator fun get(index: Int): E =
        array[actualIndexOf(index)] as E

    operator fun set(index: Int, value: E) {
        array[actualIndexOf(index)] = value
    }

    override fun contains(element: E): Boolean =
        array.contains(element) // TODO not strictly correct

    override fun containsAll(elements: Collection<E>): Boolean =
        elements.all { contains(it) }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): Iterator<E> {
        val iteratorStart = start
        val iteratorSize = actualSize
        var pointer = 0

        return object : Iterator<E> {
            override fun hasNext(): Boolean = pointer < actualSize

            override fun next(): E {
                if (iteratorStart != start || iteratorSize != actualSize) {
                    throw ConcurrentModificationException()
                }

                return get(pointer++)
            }
        }
    }

    fun slice(range: IntRange): CircularArrayList<E> {
        if (range.isEmpty()) {
            return CircularArrayList(
                array, 0, 0, 0
            )
        }

        val sliceLength = range.endInclusive - range.start + 1

        val actualStart = actualIndexOf(range.start)
        var actualEnd = actualIndexOf(range.endInclusive) + 1
        if (actualEnd >= array.size) {
            actualEnd = 0
        }

        return CircularArrayList(
            array, actualStart, actualEnd,
            sliceLength
        )
    }

//    {
//        // NOTE: this is not super efficient, but...
//        val rangeSize = range.last - range.first
//        val result = arrayOfNulls<Any>(rangeSize)
//
//        if (start < end) {
//            System.arraycopy(array, start + range.start, result, 0, rangeSize)
//        } else {
//            val firstLength = array.size - (start + range.start)
//            val secondLength = rangeSize - firstLength
//
//            System.arraycopy(array, start + range.start, result, 0, firstLength)
//            System.arraycopy(array, 0, result, firstLength, secondLength)
//        }
//
//        return Arrays.asList(result) as List<E>
//    }

    private fun actualIndexOf(virtualIndex: Int): Int {
        if (virtualIndex < 0) throw IndexOutOfBoundsException("$virtualIndex < 0")
        if (virtualIndex >= size) throw IndexOutOfBoundsException("$virtualIndex >= $size")

        val expectedIndex = start + virtualIndex
        if (start < end || expectedIndex <= array.lastIndex) {
            return expectedIndex
        } else {
            val actual = virtualIndex - (array.size - start)
            return actual
        }
    }

    private fun ensureCapacity(desiredCapacity: Int) {
        if (array.size >= desiredCapacity) {
            actualSize = desiredCapacity
            return
        }

        if (array.size < maxCapacity) {
            val newArraySize = minOf(maxCapacity,
                if (array.size < MAX_SIZE_INCREMENT) array.size * 2
                else array.size + MAX_SIZE_INCREMENT
            )
            array = array.copyOf(newArraySize)
            actualSize = desiredCapacity
        } else if (array.size == maxCapacity) {
            if (start == array.lastIndex) {
                start = 0
            } else {
                ++start
            }
        }

        if (end >= array.size) {
            end = 0
        }
    }
}
