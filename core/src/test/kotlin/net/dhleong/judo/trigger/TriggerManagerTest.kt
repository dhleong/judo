package net.dhleong.judo.trigger

import assertk.all
import assertk.assertThat
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.alias.AliasProcessingException
import net.dhleong.judo.modes.cmd.process
import net.dhleong.judo.render.FlavorableStringBuilder
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class TriggerManagerTest {
    private val triggers = TriggerManager()

    @Before fun setUp() {
        triggers.clear()
    }

    @Test fun `Detect excessive recursion`() {
        val output = "You can't take the skies from me.\n"
        triggers.define("You can't") {
            triggers.process(output)
        }

        assertThat {
            process(output)
        }.isFailure().all {
            isInstanceOf(AliasProcessingException::class.java)
            messageContains("Excessive recursion")
        }
    }

    @Suppress("SameParameterValue")
    private fun process(input: String) =
        triggers.process(
            FlavorableStringBuilder.fromString(input)
        )
}