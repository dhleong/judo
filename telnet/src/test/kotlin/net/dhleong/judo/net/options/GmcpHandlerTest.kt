package net.dhleong.judo.net.options

import assertk.assert
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import net.dhleong.judo.TestableJudoCore
import net.dhleong.judo.net.TELNET_SB
import net.dhleong.judo.net.TELNET_TELOPT_GMCP
import net.dhleong.judo.net.TelnetClient
import net.dhleong.judo.net.TelnetEvent
import net.dhleong.judo.net.TelnetOptionHandler
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * @author dhleong
 */
@Suppress("UNCHECKED_CAST")
class GmcpHandlerTest {

    lateinit var judo: TestableJudoCore
    lateinit var gmcp: GmcpHandler

    @Before
    fun setUp() {
        judo = TestableJudoCore()
        gmcp = GmcpHandler(
            judo,
            { false },
            { /* nop */ }
        )
    }

    @Test fun packageOnly() {
        gmcp.subnegotiate("net.dhleong.judo")

        val catchall = judo.raised.removeAt(0)
        assert(catchall.first).isEqualTo("GMCP")
        assert(catchall.second as Array<Any?>)
            .containsExactly("net.dhleong.judo", null)

        assert(judo.raised)
            .containsExactly(
                "GMCP:net.dhleong.judo" to null)
    }

    @Test
    fun packageOnlyWithSpaces() {
        gmcp.subnegotiate(" net.dhleong.judo ")

        val catchall = judo.raised.removeAt(0)
        assert(catchall.first).isEqualTo("GMCP")
        assert(catchall.second as Array<Any?>)
            .containsExactly("net.dhleong.judo", null)

        assert(judo.raised)
            .containsExactly(
                "GMCP:net.dhleong.judo" to null)
    }

    @Test fun packageWithPrimitiveData() {
        gmcp.subnegotiate("""net.dhleong.judo true""")

        val catchall = judo.raised.removeAt(0)
        assert(catchall.first).isEqualTo("GMCP")
        assert(catchall.second as Array<Any?>).containsExactly("net.dhleong.judo", true)

        assert(judo.raised)
            .containsExactly(
                "GMCP:net.dhleong.judo" to true)
    }

    @Test fun packageWithPrimitiveData_string() {
        gmcp.subnegotiate("""net.dhleong.judo "string"""")

        val catchall = judo.raised.removeAt(0)
        assert(catchall.first).isEqualTo("GMCP")
        assert(catchall.second as Array<Any?>).containsExactly("net.dhleong.judo", "string")

        assert(judo.raised)
            .containsExactly(
                "GMCP:net.dhleong.judo" to "string")
    }

    @Test fun packageWithMapData() {
        gmcp.subnegotiate("""net.dhleong.judo {"foo": "bar"}""")

        val catchall = judo.raised.removeAt(0)
        assert(catchall.first).isEqualTo("GMCP")
        assert(catchall.second as Array<Any?>)
            .containsExactly("net.dhleong.judo", mapOf("foo" to "bar"))

        assert(judo.raised)
            .containsExactly(
                "GMCP:net.dhleong.judo" to mapOf("foo" to "bar"))
    }

    fun TelnetOptionHandler.subnegotiate(string: String) {
        val client = TelnetClient(
            "".byteInputStream(),
            ByteArrayOutputStream()
        )

        onSubnegotiation(client, TelnetEvent(
            byteArrayOf(TELNET_SB, TELNET_TELOPT_GMCP) +
                string.toByteArray()
        ))
    }
}
