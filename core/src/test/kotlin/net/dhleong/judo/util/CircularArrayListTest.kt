package net.dhleong.judo.util

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.junit.Test

/**
 * @author dhleong
 */
class CircularArrayListTest {

    @Test fun add() {
        // this test is quite a bit too long, but it's a nice thorough
        // test of the circular-ness

        val list = CircularArrayList<Int>(10, 4)
        assertThat(list.capacity).isEqualTo(4)
        assertThat(list.size).isEqualTo(0)

        arrayOf(1, 2, 3, 4).forEach { list.add(it) }
        assertThat(list.capacity).isEqualTo(4)
        assertThat(list.size).isEqualTo(4)

        list.add(5)
        assertThat(list.capacity).isEqualTo(8)
        assertThat(list.size).isEqualTo(5)
        assertThat(list)
            .containsExactly(1, 2, 3, 4, 5)

        arrayOf(6, 7, 8).forEach { list.add(it) }
        assertThat(list.capacity).isEqualTo(8)
        assertThat(list.size).isEqualTo(8)
        assertThat(list)
            .containsExactly(1, 2, 3, 4, 5, 6, 7, 8)

        list.add(9)
        assertThat(list.capacity).isEqualTo(10)
        assertThat(list.size).isEqualTo(9)

        list.add(10)
        assertThat(list.capacity).isEqualTo(10)
        assertThat(list.size).isEqualTo(10)
        assertThat(list)
            .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        list.add(11)
        assertThat(list.capacity).isEqualTo(10)
        assertThat(list.size).isEqualTo(10)
        assertThat(list)
            .containsExactly(2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        (12..20).forEach { list.add(it) }
        assertThat(list.capacity).isEqualTo(10)
        assertThat(list.size).isEqualTo(10)
        assertThat(list)
            .containsExactly(11, 12, 13, 14, 15, 16, 17, 18, 19, 20)

        // loop 2
        list.add(21)
        assertThat(list.capacity).isEqualTo(10)
        assertThat(list.size).isEqualTo(10)
        assertThat(list)
            .containsExactly(12, 13, 14, 15, 16, 17, 18, 19, 20, 21)

        // and one for good luck
        list.add(22)
        assertThat(list.capacity).isEqualTo(10)
        assertThat(list.size).isEqualTo(10)
        assertThat(list)
            .containsExactly(13, 14, 15, 16, 17, 18, 19, 20, 21, 22)
    }

    @Test fun `insert at beginning`() {
        val list = CircularArrayList<Int>(6, 4)
        list.addAll(listOf(1, 2, 3, 4))

        // insert
        list.add(0, 5)
        assertThat(list.capacity).isEqualTo(6)
        assertThat(list.size).isEqualTo(5)
        assertThat(list)
            .containsExactly(5, 1, 2, 3, 4)

        arrayOf(6, 7, 8).forEach { list.add(0, it) }
        assertThat(list.capacity).isEqualTo(6)
        assertThat(list.size).isEqualTo(6)
        assertThat(list)
            .containsExactly(8, 7, 6, 5, 1, 2)
    }

    @Test fun get() {
        val list1 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            0, 5, size = 5
        )
        assertThat(list1[0]).isEqualTo(1)
        assertThat(list1[4]).isEqualTo(5)

        val list2 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            2, 1, size = 4
        )
        assertThat(list2[0]).isEqualTo(3)
        assertThat(list2[1]).isEqualTo(4)
        assertThat(list2[2]).isEqualTo(5)
        assertThat(list2[3]).isEqualTo(1)

        val list3 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            2, 3, size = 1
        )
        assertThat {
            list3[1]
        }.isFailure().isInstanceOf(IndexOutOfBoundsException::class.java)
    }

    @Test fun iterator() {
        val list1 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            0, 4, size = 4
        )
        assertThat(list1.iterator().asSequence().toList())
            .containsExactly(1, 2, 3, 4)

        val list2 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            2, 1, size = 4
        )
        assertThat(list2.iterator().asSequence().toList())
            .containsExactly(3, 4, 5, 1)
    }

    @Test fun lastIndex() {
        val list1 = CircularArrayList<Int>(8)
        assertThat(list1.lastIndex).isEqualTo(-1)

        // NOTE: lastIndex is always size-1...
        val list2 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            0, 4, size = 4
        )
        assertThat(list2.lastIndex).isEqualTo(3)

        val list3 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            2, 1, size = 4
        )
        assertThat(list3.lastIndex).isEqualTo(3)
    }

    @Test fun removeFirst() {
        val list1 = CircularArrayList<Int>(
            arrayOf(1),
            0, 1, size = 1
        )
        assertThat(list1.removeFirst()).isEqualTo(1)
        assertThat(list1.size).isEqualTo(0)
        assertThat(list1).containsExactly()

        list1.add(2)
        assertThat(list1.size).isEqualTo(1)
        assertThat(list1).containsExactly(2)

        val list2 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            0, 4, size = 4
        )
        assertThat(list2.removeFirst()).isEqualTo(1)
        assertThat(list2.size).isEqualTo(3)
        assertThat(list2).containsExactly(2, 3, 4)

        list2.add(6)
        assertThat(list2.size).isEqualTo(4)
        assertThat(list2).containsExactly(2, 3, 4, 6)

        val list3 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            2, 1, size = 4
        )
        assertThat(list3.removeFirst()).isEqualTo(3)
        assertThat(list3.size).isEqualTo(3)
        assertThat(list3).containsExactly(4, 5, 1)

        list3.add(6)
        assertThat(list3.size).isEqualTo(4)
        assertThat(list3).containsExactly(4, 5, 1, 6)
    }

    @Test fun removeLast() {
        val list1 = CircularArrayList<Int>(
            arrayOf(1),
            0, 1, size = 1
        )
        assertThat(list1.removeLast()).isEqualTo(1)
        assertThat(list1.size).isEqualTo(0)
        assertThat(list1).containsExactly()

        list1.add(2)
        assertThat(list1.size).isEqualTo(1)
        assertThat(list1).containsExactly(2)

        val list2 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            0, 4, size = 4
        )
        assertThat(list2.removeLast()).isEqualTo(4)
        assertThat(list2.size).isEqualTo(3)
        assertThat(list2).containsExactly(1, 2, 3)

        list2.add(6)
        assertThat(list2.size).isEqualTo(4)
        assertThat(list2).containsExactly(1, 2, 3, 6)

        val list3 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            2, 1, size = 4
        )
        assertThat(list3.removeLast()).isEqualTo(1)
        assertThat(list3.size).isEqualTo(3)
        assertThat(list3).containsExactly(3, 4, 5)

        list3.add(6)
        assertThat(list3.size).isEqualTo(4)
        assertThat(list3).containsExactly(3, 4, 5, 6)
    }

    @Test fun `Add first and remove before increasing size`() {
        val list = CircularArrayList<Int>(
            initialCapacity = 2,
            maxCapacity = 4
        )

        list.add(0, 2)
        list.add(0, 1)
        assertThat(list).containsExactly(1, 2)

        list.add(0, 0)
        assertThat(list).all {
            hasSize(3)
            containsExactly(0, 1, 2)
        }

        list.removeFirst()
        list.removeFirst()
        assertThat(list).containsExactly(2)

        list.add(3)
        list.add(4)
        list.add(5)
        assertThat(list).containsExactly(2, 3, 4, 5)
    }

    @Test fun slice() {
        val list1 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            0, 4, size = 4
        )
        assertThat(list1.slice(0..2))
            .containsExactly(1, 2, 3)

        val list2 = CircularArrayList<Int>(
            arrayOf(1, 2, 3, 4, 5),
            2, 1, size = 4
        )
        assertThat(list2.slice(0..2))
            .containsExactly(3, 4, 5)
    }

    @Test fun sliceEmpty() {
        val list1 = CircularArrayList<Int>(
            arrayOf(),
            0, 0, size = 0
        )
        @Suppress("EmptyRange")
        assertThat(list1.slice(0..-1))
            .isEmpty()
    }

}