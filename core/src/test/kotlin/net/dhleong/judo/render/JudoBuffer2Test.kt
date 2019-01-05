package net.dhleong.judo.render

import assertk.assert
import assertk.assertions.isEqualTo
import org.junit.Test

/**
 * @author dhleong
 */
class JudoBuffer2Test {
    @Test fun `append() respects newlines`() {
        val b = JudoBuffer2(IdManager())
        b.append("Take my")
        b.append(" love\n")
        b.append("Take my")
        b.append(" land\n")

        assert(b[0].toString()).isEqualTo("Take my love\n")
        assert(b[1].toString()).isEqualTo("Take my land\n")
    }
}

fun JudoBuffer2.append(text: String) =
    append(FlavorableStringBuilder.withDefaultFlavor(text))