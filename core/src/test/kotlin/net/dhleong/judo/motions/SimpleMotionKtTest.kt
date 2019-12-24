package net.dhleong.judo.motions

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * @author dhleong
 */
class SimpleMotionKtTest {

    @Test fun charMotionBack() = runBlocking<Unit> {
        val motion = charMotion(-1)
//        assertThat(motion.calculate("", 0))
//            .isEqualTo(0..-1)

        assertThat(motion.calculate("0123", 0))
            .isEqualTo(0..0)

        @Suppress("EmptyRange")
        assertThat(motion.calculate("0123", 1))
            .isEqualTo(1..0)

        @Suppress("EmptyRange")
        assertThat(motion.calculate("0123", 2))
            .isEqualTo(2..1)
    }

    @Test fun charMotionForward() = runBlocking<Unit> {
        val motion = charMotion(1)
//        assertThat(motion.calculate("", 0))
//            .isEqualTo(0..-1)

        assertThat(motion.calculate("0123", 3))
            .isEqualTo(3..4)

        assertThat(motion.calculate("0123", 2))
            .isEqualTo(2..3)

        assertThat(motion.calculate("0123", 1))
            .isEqualTo(1..2)
    }

}