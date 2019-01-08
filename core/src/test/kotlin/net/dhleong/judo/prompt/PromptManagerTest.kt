package net.dhleong.judo.prompt

import net.dhleong.judo.render.parseAnsi
import net.dhleong.judo.util.ansi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class PromptManagerTest {
    val prompts = PromptManager()
    val extractedPrompts = mutableListOf<Pair<Int, String>>()

    @Before fun setUp() {
        prompts.clear()
    }

    @Test fun stripPrompt() {
        prompts.define("^HP[$1] Ammo: $2>", "<$1> <$2>")

        // NOTE: keeping this here for historical purposes, but our new connection
        // semantics make this impossible; we will never be delivered two lines
        // in one FlavorableCharSequence like this:
//        assertThat(process("HP[||||  ] Ammo: 42> \r\nHey"))
//            .isEqualTo(" \r\nHey")

        assertThat(process("HP[||||  ] Ammo: 42> \n"))
            .isEqualTo(" \n")

        assertThat(extractedPrompts)
            .containsExactly(0 to "<||||  > <42>")
    }

    @Test fun stripAnsiPrompt() {

        prompts.define("^HP[$1] Ammo: $2>", "<$1> <$2>")
        assertThat(process("${ansi(1, 6)}HP[${ansi(1, 2)}||||  ] Ammo: 42> \n"))
            .isEqualTo(" \n")
        assertThat(extractedPrompts)
            .containsExactly(0 to "<||||  > <42>")
    }

    private fun process(input: String): String =
        prompts.process(input.parseAnsi()) { index, prompt ->
            extractedPrompts.add(index to prompt)
        }.toString()
}