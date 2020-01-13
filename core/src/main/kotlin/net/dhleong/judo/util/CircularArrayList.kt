package net.dhleong.judo.util

import net.dhleong.judo.render.BufferStorage
import java.util.ConcurrentModificationException

private const val MAX_SIZE_INCREMENT = 2048

/**
 * Size-limited FIFO array-list
 * @author dhleong
 */
class CircularArrayList<E> : AbstractMutableList<E>, Collection<E>, BufferStorage<E> {

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
        array = arrayOfNulls(initialCapacity)
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

    override fun add(element: E): Boolean {
        ensureCapacity(actualSize + 1) {
            if (start == array.lastIndex) {
                start = 0
            } else {
                ++start
            }
        }

        if (end >= array.size) {
            end = 0
        }
        array[end] = element

        ++end
        return true
    }

    override fun add(index: Int, element: E) {
        when (index) {
            size -> add(element)
            0 -> addFirst(element)
            else -> throw UnsupportedOperationException()
        }
    }

    private fun addFirst(element: E) {
        ensureCapacity(actualSize + 1) {
            if (end == 0) {
                end = array.lastIndex
            } else {
                --end
            }
        }

        --start
        if (start < 0) {
            start = array.lastIndex
        }
        array[start] = element

    }

    override fun clear() {
        actualSize = 0
        start = 0
        end = 0
    }

    override fun removeAt(index: Int): E = when (index) {
        0 -> removeFirst()
        lastIndex -> removeLast()
        else -> throw UnsupportedOperationException()
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun get(index: Int): E =
        array[actualIndexOf(index)] as? E
            ?: throw IllegalStateException("Requesting $index of $size but it's null")

    @Suppress("UNCHECKED_CAST")
    override operator fun set(index: Int, element: E): E {
        val actual = actualIndexOf(index)
        val old = array[actual]
        array[actual] = element
        return old as E
    }

    override fun contains(element: E): Boolean =
        array.contains(element) // TODO not strictly correct

    override fun containsAll(elements: Collection<E>): Boolean =
        elements.all { contains(it) }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<E> {
        val iteratorStart = start
        val iteratorSize = actualSize
        var pointer = 0

        return object : Iterator<E>, MutableIterator<E> {
            override fun hasNext(): Boolean = pointer < actualSize

            override fun next(): E {
                if (iteratorStart != start || iteratorSize != actualSize) {
                    throw ConcurrentModificationException()
                }

                return get(pointer++)
            }

            override fun remove() {
                throw UnsupportedOperationException()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun removeFirst(): E {
        if (actualSize == 0) throw NoSuchElementException()
        else if (actualSize == 1) {
            return get(0).also { clear() }
        }

        val first = array[start]
        start = actualIndexOf(1)
        --actualSize
        return first as E
    }

    @Suppress("UNCHECKED_CAST")
    override fun removeLast(): E {
        if (actualSize == 0) throw NoSuchElementException()
        val newEnd = actualIndexOf(actualSize - 1)
        val last = array[newEnd]
        end = newEnd
        --actualSize
        return last as E
    }

    fun slice(range: IntRange): CircularArrayList<E> {
        if (range.isEmpty()) {
            return CircularArrayList(
                array, 0, 0, 0
            )
        }

        val sliceLength = range.last - range.first + 1

        val actualStart = actualIndexOf(range.first)
        var actualEnd = actualIndexOf(range.last) + 1
        if (actualEnd >= array.size) {
            actualEnd = 0
        }

        return CircularArrayList(
            array, actualStart, actualEnd,
            sliceLength
        )
    }

    private fun actualIndexOf(virtualIndex: Int): Int {
        if (virtualIndex < 0) throw IndexOutOfBoundsException("$virtualIndex < 0")
        if (virtualIndex >= size) throw IndexOutOfBoundsException("$virtualIndex >= $size")

        val expectedIndex = start + virtualIndex
        return if (start < end || expectedIndex <= array.lastIndex) {
            expectedIndex
        } else {
            virtualIndex - (array.size - start)
        }
    }

    private inline fun ensureCapacity(desiredCapacity: Int, onCircle: () -> Unit) {
        if (array.size >= desiredCapacity) {
            actualSize = desiredCapacity
            return
        }

        if (array.size < maxCapacity) {
            val newArraySize = minOf(
                maxCapacity,
                if (array.size < MAX_SIZE_INCREMENT) array.size * 2
                else array.size + MAX_SIZE_INCREMENT
            )
            val old = array
            val new = array.copyOf(newArraySize)
            if (start >= end) {
                old.copyInto(new, 0, startIndex = start)
                old.copyInto(new,
                    destinationOffset = old.size - start,
                    startIndex = 0,
                    endIndex = end
                )
                start = 0
                end = actualSize
            }
            array = new
            actualSize = desiredCapacity
        } else if (array.size == maxCapacity) {
            onCircle()
        }
    }
}
