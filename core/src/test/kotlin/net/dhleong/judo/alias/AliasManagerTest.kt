package net.dhleong.judo.alias

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Ignore
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

        assertThat(aliases.process("this firefly is a fireflyfire"))
            .isEqualTo("this serenity is a fireflyfire")
    }

    @Test fun fnReplace() {
        var count = 0
        aliases.define("firefly", { "firefly_${++count}" })

        assertThat(aliases.process("this firefly that firefly"))
            .isEqualTo("this firefly_1 that firefly_2")
    }

    @Test fun detectInfiniteRecursion() {
        aliases.define("cool", "VERY cool")

        assertThatThrownBy {
            aliases.process("This is cool")
        }.isInstanceOf(AliasProcessingException::class.java)
            .hasMessageContaining("Infinite recursion")
    }

    @Ignore("TODO")
    @Test fun replaceWithOneVar() {
    }

    @Ignore("TODO")
    @Test fun replaceWithOneVar_missing() {
    }

    @Ignore("TODO")
    @Test fun replaceWithTwoVars() {
    }

    @Ignore("TODO")
    @Test fun replaceWithTwoVars_missing() {
    }


}