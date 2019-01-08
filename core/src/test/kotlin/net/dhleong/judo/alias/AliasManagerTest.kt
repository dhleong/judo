package net.dhleong.judo.alias

import net.dhleong.judo.render.FlavorableStringBuilder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
class AliasManagerTest {

    val aliases = AliasManager()

    @Before fun setUp() {
        aliases.clear()
    }

    @Test fun simpleReplace() {
        aliases.define("firefly", "serenity")

        assertThat(process("this firefly is a fireflyfire"))
            .isEqualTo("this serenity is a fireflyfire")
    }

    @Test fun onlyMatchStart() {
        aliases.define("^firefly", "serenity")

        assertThat(process("firefly is a firefly"))
            .isEqualTo("serenity is a firefly")
    }

    @Test fun expandingIsntRecursive() {
        aliases.define("fire", "firefly")

//        Pattern.compile("(?<=^|\\s)(fire)(?=$|\\s)")
        assertThat(process("fire is a firefly"))
            .isEqualTo("firefly is a firefly")
    }

    @Test fun onlyMatchStartExpandingIsntRecursive() {
        aliases.define("^fire", "firefly")

        assertThat(process("fire is a firefly"))
            .isEqualTo("firefly is a firefly")
    }

    @Test fun undefine() {
        aliases.define("^firefly", "serenity")
        assertThat(aliases.hasAliasFor("^firefly")).isTrue()

        aliases.undefine("^firefly")
        assertThat(aliases.hasAliasFor("^firefly")).isFalse()

        assertThat(process("firefly is a firefly"))
            .isEqualTo("firefly is a firefly")
    }


    @Test fun fnReplace() {
        var count = 0
        aliases.define("firefly") { "firefly_${++count}" }

        assertThat(process("this firefly that firefly"))
            .isEqualTo("this firefly_1 that firefly_2")
    }

    @Test fun detectInfiniteRecursion() {
        aliases.define("cool", "VERY cool")

        assertThatThrownBy {
            process("This is cool")
        }.isInstanceOf(AliasProcessingException::class.java)
            .hasMessageContaining("Infinite recursion")
    }

    @Test fun replaceWithOneVar() {
        aliases.define("admire $1", "This $1 is VERY cool")

        assertThat(process("admire shiny"))
            .isEqualTo("This shiny is VERY cool")
    }

    @Test fun replaceWithOneVar_func() {
        aliases.define("admire $1") { args -> "This ${args[0]} is VERY cool"}

        assertThat(process("admire shiny"))
            .isEqualTo("This shiny is VERY cool")
    }

    @Test fun replaceWithOneVar_missing() {
        aliases.define("admire $1", "This $1 is VERY cool")

        assertThat(process("admire "))
            .isEqualTo("admire ")
    }

    @Test fun replaceWithTwoVars() {
        aliases.define("admire $1 with $2", "This $1 is VERY $2")

        assertThat(process("admire shiny with awesome"))
            .isEqualTo("This shiny is VERY awesome")
    }

    @Test fun replaceWithTwoVars_missing() {
        aliases.define("admire $1 with $2", "This $1 is VERY $2")

        assertThat(process("admire shiny awesome"))
            .isEqualTo("admire shiny awesome")
    }

    private fun process(input: String): String =
        aliases.process(
            FlavorableStringBuilder.fromString(input)
        ).toString()
}