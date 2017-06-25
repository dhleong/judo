package net.dhleong.judo.logging

import java.io.Writer

/**
 * @author dhleong
 */
class RawLogFormatter : BasePlainTextFormatter() {
    override val format = ILogManager.Format.RAW

    override fun writeLine(input: CharSequence, out: Writer) {
        out.appendln(input)
    }
}