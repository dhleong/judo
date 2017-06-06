package net.dhleong.judo.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

        arrayOf(1, 2, 3, 4).forEach(list::add)
        assertThat(list.capacity).isEqualTo(4)
        assertThat(list.size).isEqualTo(4)

        list.add(5)
        assertThat(list.capacity).isEqualTo(8)
        assertThat(list.size).isEqualTo(5)
        assertThat(list)
            .containsExactly(1, 2, 3, 4, 5)

        arrayOf(6, 7, 8).forEach(list::add)
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

    @Test fun get() {
        val list1 = CircularArrayList<Int>(
            arrayOf<Any?>(1, 2, 3, 4, 5),
            0, 5, size = 5
        )
        assertThat(list1[0]).isEqualTo(1)
        assertThat(list1[4]).isEqualTo(5)

        val list2 = CircularArrayList<Int>(
            arrayOf<Any?>(1, 2, 3, 4, 5),
            2, 1, size = 4
        )
        assertThat(list2[0]).isEqualTo(3)
        assertThat(list2[1]).isEqualTo(4)
        assertThat(list2[2]).isEqualTo(5)
        assertThat(list2[3]).isEqualTo(1)

        val list3 = CircularArrayList<Int>(
            arrayOf<Any?>(1, 2, 3, 4, 5),
            2, 3, size = 1
        )
        assertThatThrownBy {
            list3[1]
        }.isInstanceOf(IndexOutOfBoundsException::class.java)
    }

    @Test fun iterator() {
        val list1 = CircularArrayList<Int>(
            arrayOf<Any?>(1, 2, 3, 4, 5),
            0, 4, size = 4
        )
        assertThat(list1.iterator())
            .containsExactly(1, 2, 3, 4)

        val list2 = CircularArrayList<Int>(
            arrayOf<Any?>(1, 2, 3, 4, 5),
            2, 1, size = 4
        )
        assertThat(list2.iterator())
            .containsExactly(3, 4, 5, 1)
    }

    @Test fun slice() {
        val list1 = CircularArrayList<Int>(
            arrayOf<Any?>(1, 2, 3, 4, 5),
            0, 4, size = 4
        )
        assertThat(list1.slice(0..2))
            .containsExactly(1, 2, 3)

        val list2 = CircularArrayList<Int>(
            arrayOf<Any?>(1, 2, 3, 4, 5),
            2, 1, size = 4
        )
        assertThat(list2.slice(0..2))
            .containsExactly(3, 4, 5)
    }

    @Test fun sliceEmpty() {
        val list1 = CircularArrayList<Int>(
            arrayOf<Any?>(),
            0, 0, size = 0
        )
        assertThat(list1.slice(0..-1))
            .isEmpty()
    }

}