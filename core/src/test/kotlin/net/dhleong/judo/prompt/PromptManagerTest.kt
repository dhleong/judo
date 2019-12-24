package net.dhleong.judo.prompt

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import net.dhleong.judo.render.parseAnsi
import net.dhleong.judo.util.ansi
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class PromptManagerTest {
    private val prompts = PromptManager()
    private val extractedPrompts = mutableListOf<Triple<Int, String, Int>>()

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
            .containsExactly(-1 to "<||||  > <42>" to 0)
    }

    @Test fun stripAnsiPrompt() {
        prompts.define("^HP[$1] Ammo: $2>", "<$1> <$2>")
        assertThat(process("${ansi(1, 6)}HP[${ansi(1, 2)}||||  ] Ammo: 42> \n"))
            .isEqualTo(" \n")
        assertThat(extractedPrompts)
            .containsExactly(-1 to "<||||  > <42>" to 0)
    }

    @Test fun `Switch between groups`() {
        prompts.define("^HP[$1] Ammo: $2>", "<$1> Combat: <$2>")
        prompts.define("^HP[$1]>", "<$1>")

        assertThat(process("HP[||||  ] Ammo: 42> \n")).isEqualTo(" \n")
        assertThat(extractedPrompts).containsExactly(-1 to "<||||  > Combat: <42>" to 0)
        extractedPrompts.clear()

        assertThat(process("HP[||||  ]>\n")).isEqualTo("")
        assertThat(extractedPrompts).containsExactly(-2 to "<||||  >" to 0)
    }

    @Test fun `Multiple prompts in a group`() {
        prompts.define("^HP[$1]>", "HP: <$1>", group = 1)
        prompts.define("^Ammo: $1>", "Ammo: $1", group = 1)

        assertThat(process("HP[||||  ]>\n")).isEqualTo("")
        assertThat(extractedPrompts).containsExactly(1 to "HP: <||||  >" to 0)
        extractedPrompts.clear()

        assertThat(process("Ammo: 42>\n")).isEqualTo("")
        assertThat(extractedPrompts).containsExactly(1 to "Ammo: 42" to 1)
    }

    @Test fun `Strip prompts that resolve to empty`() {
        var hp = ""
        var ammo = ""
        prompts.define("^HP[$1]>", { matches ->
            hp = matches[0]
            null // render nothing
        }, group = 1)
        prompts.define("^Ammo[$1]>", { matches ->
            ammo = matches[0]
            null // render nothing
        }, group = 1)
        prompts.define("^MP[$1]>", { (mp) ->
            "HP<$hp> MP<$mp> Ammo<$ammo>"
        }, group = 1)

        // since HP resolves to the empty string, we don't count it
        assertThat(process("HP[10]>\n")).isEqualTo("")
        assertThat(extractedPrompts).isEmpty()
        assertThat(hp).isEqualTo("10")

        // note that even though HP is first, because it doesn't produce output
        // it is not counted when computing index
        assertThat(process("MP[20]>\n")).isEqualTo("")
        assertThat(extractedPrompts).containsExactly(1 to "HP<10> MP<20> Ammo<>" to 0)
        extractedPrompts.clear()

        // Ammo also doesn't produce any output, but it should re-render MP
        // (and not cause an infinite loop with HP)
        assertThat(process("Ammo[11]>\n")).isEqualTo("")
        assertThat(extractedPrompts).containsExactly(1 to "HP<10> MP<20> Ammo<11>" to 0)
        extractedPrompts.clear()

        // when we see HP again, since it doesn't produce output,
        // the OTHER prompts are re-triggered
        assertThat(process("HP[42]>\n")).isEqualTo("")
        assertThat(hp).isEqualTo("42")
        assertThat(extractedPrompts).containsExactly(1 to "HP<42> MP<20> Ammo<11>" to 0)
    }

    private fun process(input: String): String =
        prompts.process(input.parseAnsi()) { group, prompt, index ->
            extractedPrompts.add(Triple(group, prompt, index))
        }.toString()
}

private infix fun Pair<Int, String>.to(third: Int) =
    Triple(first, second, third)