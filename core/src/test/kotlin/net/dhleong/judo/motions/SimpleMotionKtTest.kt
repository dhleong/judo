package net.dhleong.judo.motions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
class SimpleMotionKtTest {
    val readKey: () -> KeyStroke = { KeyStroke.getKeyStroke(' ') }

    @Test fun charMotionBack() {
        val motion = charMotion(-1)
//        assertThat(motion.calc("", 0))
//            .isEqualTo(0..-1)

        assertThat(motion.calc("0123", 0))
            .isEqualTo(0..0)

        assertThat(motion.calc("0123", 1))
            .isEqualTo(1..0)

        assertThat(motion.calc("0123", 2))
            .isEqualTo(2..1)
    }

    @Test fun charMotionForward() {
        val motion = charMotion(1)
//        assertThat(motion.calc("", 0))
//            .isEqualTo(0..-1)

        assertThat(motion.calc("0123", 3))
            .isEqualTo(3..4)

        assertThat(motion.calc("0123", 2))
            .isEqualTo(2..3)

        assertThat(motion.calc("0123", 1))
            .isEqualTo(1..2)
    }

    fun Motion.calc(buffer: CharSequence, cursor: Int) =
        calculate(readKey, buffer, cursor)
}