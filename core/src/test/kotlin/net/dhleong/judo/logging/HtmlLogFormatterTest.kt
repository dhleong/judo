package net.dhleong.judo.logging

import net.dhleong.judo.util.ESCAPE_CHAR
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

    @Test fun entities() {
        val out = withStringWriter {
            formatter.writeLine("<mreynolds>", it)
        }

        assertThat(out).isEqualTo("&lt;mreynolds&gt;")
    }

    @Test fun simpleColor() {
        val out = withStringWriter {
            formatter.writeLine("$ESCAPE_CHAR[37;42mColor", it)
        }

        assertThat(out).isEqualTo(
            """</span><span class="d7 b42">Color"""
        )
    }

    @Test fun color256_000() {
        val out = withStringWriter {
            formatter.writeLine("$ESCAPE_CHAR[38;5;16mColor", it)
        }

        assertThat(out).isEqualTo(
            """</span><span style="color: #000;">Color"""
        )
    }

    @Test fun color256_FFF() {
        val out = withStringWriter {
            formatter.writeLine("$ESCAPE_CHAR[38;5;231mColor", it)
        }

        assertThat(out).isEqualTo(
            """</span><span style="color: #FFF;">Color"""
        )
    }

    @Test fun color256_0FF() {
        val out = withStringWriter {
            formatter.writeLine("$ESCAPE_CHAR[38;5;51mColor", it)
        }

        assertThat(out).isEqualTo(
            """</span><span style="color: #0FF;">Color"""
        )
    }

    @Test fun grayScale_8() {
        val out = withStringWriter {
            formatter.writeLine("$ESCAPE_CHAR[38;5;232mColor", it)
        }

        assertThat(out).isEqualTo(
            """</span><span style="color: rgb(8,8,8);">Color"""
        )
    }

    @Test fun grayScale_238() {
        val out = withStringWriter {
            formatter.writeLine("$ESCAPE_CHAR[38;5;255mColor", it)
        }

        assertThat(out).isEqualTo(
            """</span><span style="color: rgb(238,238,238);">Color"""
        )
    }

    @Test fun trueColor() {
        val out = withStringWriter {
            formatter.writeLine("$ESCAPE_CHAR[38;2;15;14;13mColor", it)
        }

        assertThat(out).isEqualTo(
            """</span><span style="color: rgb(15,14,13);">Color"""
        )
    }

    @Test fun italicTrueColor() {
        val out = withStringWriter {
            formatter.writeLine("$ESCAPE_CHAR[3;38;2;15;14;13mColor", it)
        }

        assertThat(out).isEqualTo(
            """</span><span class="i" style="color: rgb(15,14,13);">Color"""
        )
    }

    inline fun withStringWriter(block: (Writer) -> Unit): String {
        val writer = StringWriter()
        block(writer)
        return writer.toString().trim()
    }
}