package net.dhleong.judo.render

import assertk.all
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasLength
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.startsWith
import org.junit.Test
import java.io.File

private const val LOREM_SIZE = 38272

/**
 * @author dhleong
 */
class DiskBackedListTest {

    @Test(timeout = 200) fun `count lines`() {
        assertThat(lorem()).hasSize(LOREM_SIZE)
    }

    @Test(timeout = 200) fun `read last line`() {
        assertThat(lorem().last()).isNotNull().transform { it.toString() }.all {
            hasLength(564)
            startsWith("In id pulvinar enim.")
            endsWith(", dapibus sed elit.\n")
        }
    }

    @Test(timeout = 200) fun `read nth line`() {
        val list = lorem()
        assertThat(list[list.lastIndex - 4]).isNotNull().transform { it.toString() }.all {
            hasLength(763)
            startsWith("Ut lacinia nibh odio,")
            endsWith(", tristique porta purus lobortis eget.\n")
        }
    }

    @Test fun `Persist changes`() {
        val f = File(".tmp")
        f.deleteOnExit()

        val list = DiskBackedList(f)
        sequenceOf(
            "Take my love",
            "Take my land",
            "Take me where I cannot stand"
        ).map { it.toFlavorable() }
            .forEach {
                list.add(it)
                list.flush()
            }

        val fromDisk = DiskBackedList(f)
        assertThat(fromDisk).all {
            hasSize(3)
            transform("nth(2)") { it[2].toString().trim() }
                .isEqualTo("Take me where I cannot stand")
        }
    }

}

private fun lorem() = DiskBackedList(
    File("../lorem.txt"),
    maxCapacity = LOREM_SIZE
)