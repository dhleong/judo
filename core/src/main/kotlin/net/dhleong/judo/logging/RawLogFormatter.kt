package net.dhleong.judo.logging

import net.dhleong.judo.net.toAnsi
import net.dhleong.judo.render.FlavorableCharSequence
import java.io.Writer

/**
 * @author dhleong
 */
class RawLogFormatter : BasePlainTextFormatter() {
    override val format = ILogManager.Format.RAW

    override fun writeLine(input: FlavorableCharSequence, out: Writer) {
        out.appendln(input.toAnsi())
    }
}