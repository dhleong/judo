package net.dhleong.judo.net

import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.assertThat
import org.apache.commons.net.telnet.TelnetOptionHandler
import org.junit.Before
import org.junit.Test

/**
 * @author dhleong
 */
@Suppress("UNCHECKED_CAST")
class GmcpHandlerTest {

    lateinit var judo: TestableJudoCore
    lateinit var gmcp: GmcpHandler

    @Before fun setUp() {
        judo = TestableJudoCore()
        gmcp = GmcpHandler(
            judo,
            { false },
            { _ -> }
        )
    }

    @Test fun packageOnly() {
        gmcp.subnegotiate("net.dhleong.judo")

        val catchall = judo.raised.removeAt(0)
        assertThat(catchall.first).isEqualTo("GMCP")
        assertThat(catchall.second as Array<Any?>)
            .containsExactly("net.dhleong.judo", null)

        assertThat(judo.raised)
            .containsExactly(
                "GMCP:net.dhleong.judo" to null)
    }

    @Test fun packageOnlyWithSpaces() {
        gmcp.subnegotiate(" net.dhleong.judo ")

        val catchall = judo.raised.removeAt(0)
        assertThat(catchall.first).isEqualTo("GMCP")
        assertThat(catchall.second as Array<Any?>)
            .containsExactly("net.dhleong.judo", null)

        assertThat(judo.raised)
            .containsExactly(
                "GMCP:net.dhleong.judo" to null)
    }

    @Test fun packageWithPrimitiveData() {
        gmcp.subnegotiate("""net.dhleong.judo true""")

        val catchall = judo.raised.removeAt(0)
        assertThat(catchall.first).isEqualTo("GMCP")
        assertThat(catchall.second as Array<Any?>).containsExactly("net.dhleong.judo", true)

        assertThat(judo.raised)
            .containsExactly(
                "GMCP:net.dhleong.judo" to true)
    }

    @Test fun packageWithPrimitiveData_string() {
        gmcp.subnegotiate("""net.dhleong.judo "string"""")

        val catchall = judo.raised.removeAt(0)
        assertThat(catchall.first).isEqualTo("GMCP")
        assertThat(catchall.second as Array<Any?>).containsExactly("net.dhleong.judo", "string")

        assertThat(judo.raised)
            .containsExactly(
                "GMCP:net.dhleong.judo" to "string")
    }

    @Test fun packageWithMapData() {
        gmcp.subnegotiate("""net.dhleong.judo {"foo": "bar"}""")

        val catchall = judo.raised.removeAt(0)
        assertThat(catchall.first).isEqualTo("GMCP")
        assertThat(catchall.second as Array<Any?>)
            .containsExactly("net.dhleong.judo", mapOf("foo" to "bar"))

        assertThat(judo.raised)
            .containsExactly(
                "GMCP:net.dhleong.judo" to mapOf("foo" to "bar"))
    }

    fun TelnetOptionHandler.subnegotiate(string: String) {
        val intArray = (arrayOf(TELNET_TELOPT_GMCP) + string.map { it.toInt() }).toIntArray()
        answerSubnegotiation(intArray, intArray.size)
    }
}