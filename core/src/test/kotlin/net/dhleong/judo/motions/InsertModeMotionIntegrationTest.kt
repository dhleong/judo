package net.dhleong.judo.motions

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.dhleong.judo.input.keys
import net.dhleong.judo.setInput
import net.dhleong.judo.type
import org.junit.Test

/**
 * @author dhleong
 */
class InsertModeMotionIntegrationTest : AbstractMotionIntegrationTest() {

    @Test fun `Move to end`() {
        judo.setInput("word w@rd2 word3", 1)
        judo.type(keys("a<c-e>"))

        assertThat(renderer.inputLine)
            .isEqualTo("word w@rd2 word3" to 16)
    }

}