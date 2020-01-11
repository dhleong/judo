package net.dhleong.judo.render

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.dhleong.judo.EmptyStateMap
import net.dhleong.judo.hasLines
import net.dhleong.judo.matchesLinesExactly
import org.junit.Test
import java.io.File

/**
 * @author dhleong
 */
class JudoBufferTest {
    @Test fun `append() respects newlines`() {
        val b = JudoBuffer(IdManager(), EmptyStateMap)
        b.append("Take my")
        b.append(" love\n")
        b.append("Take my")
        b.append(" land\n")

        assertThat(b[0].toString()).isEqualTo("Take my love\n")
        assertThat(b[1].toString()).isEqualTo("Take my land\n")
    }

    @Test fun `append() splits on newlines`() {
        val b = JudoBuffer(IdManager(), EmptyStateMap)
        b.append("Take\nmy\nlove\n")

        assertThat(b).hasLines(
            "Take\n",
            "my\n",
            "love\n"
        )
    }

    @Test fun `Persist and unpersist`() {
        val f = File(".JudoBufferTest.persist.judo")
        f.deleteOnExit()
        f.delete()
        f.writeText("""
            Take my love
            Take my land
        """.trimIndent())

        val b = JudoBuffer(IdManager(), EmptyStateMap).apply {
            setPersistent(f)

            assertThat(this).matchesLinesExactly(
                "Take my love\n",
                "Take my land\n",
                Regex("""\^\^\^ Loaded 2 lines .*\n"""),
                "\n"
            )

            setNotPersistent()

            // re-connect
            setPersistent(f)
        }

        assertThat(b).matchesLinesExactly(
            "Take my love\n",
            "Take my land\n",
            Regex("""\^\^\^ Loaded 2 lines .*\n"""),
            "\n",
            Regex("""\^\^\^ Loaded 4 lines .*\n"""),
            "\n"
        )

        b.apply {
            setNotPersistent()
            setPersistent(f)
        }

        assertThat(b).matchesLinesExactly(
            "Take my love\n",
            "Take my land\n",
            Regex("""\^\^\^ Loaded 2 lines .*\n"""),
            "\n",
            Regex("""\^\^\^ Loaded 4 lines .*\n"""),
            "\n",
            Regex("""\^\^\^ Loaded 6 lines .*\n"""),
            "\n"
        )
    }

    @Test fun `Connect and reconnect a lot`() {
        val f = File(".JudoBufferTest.reconnect.judo")
        f.deleteOnExit()
        f.delete()
        f.writeText("""
            Take my love
            Take my land
        """.trimIndent())

        JudoBuffer(IdManager(), EmptyStateMap).apply {
            for (i in 0 until 50) {
                setPersistent(f)

                // read from the buffer
                for (j in 0 until size) {
                    get(j)
                }

                appendLine("Connected.".toFlavorable())
                appendLine("Take me where I cannot stand".toFlavorable())

                // read from the buffer
                for (j in 0 until size) {
                    get(j)
                }

                setNotPersistent()
                appendLine("Disconnected.".toFlavorable())
            }
        }
    }
}

fun JudoBuffer.append(text: String) =
    append(FlavorableStringBuilder.withDefaultFlavor(text))