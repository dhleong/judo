package net.dhleong.judo.render

import assertk.all
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasLength
import assertk.assertions.hasSize
import assertk.assertions.isNotNull
import assertk.assertions.startsWith
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

private const val LIST_SIZE = 38272

/**
 * @author dhleong
 */
class DiskBackedListTest {

    private lateinit var list: DiskBackedList

    @Before fun setUp() {
        list = DiskBackedList(File("../lorem.txt"), maxCapacity = LIST_SIZE)
    }

    @After fun tearDown() {
        list.close()
    }

    @Test(timeout = 200) fun `count lines`() {
        assertThat(list).hasSize(LIST_SIZE)
    }

    @Test(timeout = 200) fun `read last line`() {
        assertThat(list.last()).isNotNull().transform { it.toString() }.all {
            hasLength(564)
            startsWith("In id pulvinar enim.")
            endsWith(", dapibus sed elit.\n")
        }
    }

    @Test(timeout = 200) fun `read nth line`() {
        assertThat(list[list.lastIndex - 4]).isNotNull().transform { it.toString() }.all {
            hasLength(763)
            startsWith("Ut lacinia nibh odio,")
            endsWith(", tristique porta purus lobortis eget.\n")
        }
    }

}