package net.dhleong.judo.logging

import net.dhleong.judo.render.FlavorableCharSequence
import java.io.Writer

/**
 * @author dhleong
 */
class PlainLogFormatter : BasePlainTextFormatter() {
    override val format = ILogManager.Format.PLAIN

    override fun writeLine(input: FlavorableCharSequence, out: Writer) {
        out.appendln(input)
    }
}