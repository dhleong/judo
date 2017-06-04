package net.dhleong.judo

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * @author dhleong
 */
class JudoCoreTest {

    val outputLines = mutableListOf<Pair<String, Boolean>>()

    val renderer: JudoRenderer = Proxy.newProxyInstance(
        ClassLoader.getSystemClassLoader(),
        arrayOf(JudoRenderer::class.java)
    ) { _, method, args ->
        when (method.name) {
            "appendOutput" -> {
                val line = args[0] as String
                val isPartial = args[1] as Boolean

                outputLines.add(line to isPartial)
            }

            "inTransaction" -> {
                @Suppress("UNCHECKED_CAST")
                val block = args[0] as () -> Unit
                block()
            }

            else -> null // ignore
        }
    } as JudoRenderer

    lateinit var judo: JudoCore

    @Before fun setUp() {
        outputLines.clear()
        judo = JudoCore(renderer)
    }

    @Test fun appendOutput() {
        judo.appendOutput("\r\nTake my love,\r\n\r\nTake my land,\r\nTake me")

        assertThat(outputLines)
            .containsExactly(
                ""              to false,
                "Take my love," to false,
                ""              to false,
                "Take my land," to false,
                "Take me"       to true
            )
    }

    @Test fun appendOutput_newlineOnly() {
        judo.appendOutput("\nTake my love,\nTake my land,\n\nTake me")

        assertThat(outputLines)
            .containsExactly(
                ""              to false,
                "Take my love," to false,
                "Take my land," to false,
                ""              to false,
                "Take me"       to true
            )
    }

    @Test fun appendOutput_fancy() {

        judo.appendOutput(
            "\n\r${0x27}[1;30m${0x27}[1;37mTake my love," +
                "\n\r${0x27}[1;30m${0x27}[1;37mTake my land,")

        assertThat(outputLines)
            .containsExactly(
                ""                                          to false,
                "${0x27}[1;30m${0x27}[1;37mTake my love,"   to false,
                "${0x27}[1;30m${0x27}[1;37mTake my land,"   to true
            )
    }

    @Test fun appendOutput_midPartial() {
        judo.appendOutput("\n\rTake my love,\n\rTake my")
        judo.appendOutput(" land,\n\rTake me where...\n\r")
        judo.appendOutput("I don't care, I'm still free")

        assertThat(outputLines)
            .containsExactly(
                ""                              to false,
                "Take my love,"                 to false,
                "Take my"                       to true,
                " land,"                        to false,
                "Take me where..."              to false,
                "I don't care, I'm still free"  to true
            )
    }
}

fun JudoCore.appendOutput(string: String) {
    appendOutput(string.toCharArray(), string.length)
}
