package net.dhleong.judo.render

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.dhleong.judo.EmptyStateMap
import net.dhleong.judo.hasLines
import org.junit.Test

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
}

fun JudoBuffer.append(text: String) =
    append(FlavorableStringBuilder.withDefaultFlavor(text))