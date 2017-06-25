package net.dhleong.judo.logging

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.StringWriter
import java.io.Writer

/**
 * @author dhleong
 */
class HtmlLogFormatterTest {

    lateinit var formatter: HtmlLogFormatter

    @Before fun setUp() {
        formatter = HtmlLogFormatter()
    }

    @Test fun appendEntities() {
        val out = withStringWriter {
            formatter.writeLine("<mreynolds>", it)
        }

        assertThat(out).isEqualTo("&lt;mreynolds&gt;")
    }

    inline fun withStringWriter(block: (Writer) -> Unit): String {
        val writer = StringWriter()
        block(writer)
        return writer.toString().trim()
    }
}