package net.dhleong.judo.render

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
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

    @Test fun `Support reading all persisted lines`() {
        val f = File("manually-persisted.tmp")
        f.deleteOnExit()
        f.writeText("""
            Take my love
            Take my land
        """.trimIndent())

        assertThat(DiskBackedList(f)).all {
            hasSize(2)
            containsExactly(
                "Take my love\n".toFlavorable(),
                "Take my land\n".toFlavorable()
            )
        }
    }

    @Test fun `Persist changes one by one`() {
        val f = File("one-by-one.tmp")
        f.deleteOnExit()

        val list = DiskBackedList(f)
        sequenceOf(
            "Take my love",
            "Take my land",
            "Take me where I cannot stand"
        ).map { it.toFlavorable() }
            .forEach {
                list.add(it)
                list.save()
            }

        val fromDisk = DiskBackedList(f)
        assertThat(fromDisk).all {
            hasSize(3)
            transform("nth(2)") { it[2].toString().trim() }
                .isEqualTo("Take me where I cannot stand")
        }
    }

    @Test fun `Persist changes in batch`() {
        val f = File("batch.tmp")
        f.deleteOnExit()

        val list = DiskBackedList(f)
        sequenceOf(
            "Take my love\n",
            "Take my land\n",
            "Take me where I cannot stand\n"
        ).map { it.toFlavorable() }
            .forEach { list.add(it) }
        list.save()

        // list should be unchanged
        assertThat(list.map { it.toString() }).all {
            hasSize(3)
            containsExactly(
                "Take my love\n",
                "Take my land\n",
                "Take me where I cannot stand\n"
            )
        }

        val fromDisk = DiskBackedList(f)
        assertThat(fromDisk).all {
            hasSize(3)
            transform("nth(2)") { it[2].toString().trim() }
                .isEqualTo("Take me where I cannot stand")
        }
    }

    @Test fun `Persist as circled list, dropping old persisted lines`() {
        val f = File(".DBLTest.circled.judo")
        f.deleteOnExit()
        f.delete()
        f.writeText("""
            Take my love
            Take my land
        """.trimIndent())

        val fromDisk = DiskBackedList(f, maxCapacity = 2)
        fromDisk.add("Take me where".toFlavorable())
        fromDisk.flush()

        assertThat(f.readLines().map { it.parseAnsi().toString() }).containsExactly(
            "Take my land",
            "Take me where"
        )

        val reconstituted = DiskBackedList(f)
        assertThat(reconstituted).containsExactly(
            "Take my land\n".toFlavorable(),
            "Take me where\n".toFlavorable()
        )
    }
}

private fun lorem() = DiskBackedList(
    File("../lorem.txt"),
    maxCapacity = LOREM_SIZE
)