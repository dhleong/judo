package net.dhleong.judo.logging

import net.dhleong.judo.render.OutputLine
import net.dhleong.judo.util.stripAnsi
import java.io.Writer

/**
 * @author dhleong
 */
class PlainLogFormatter : BasePlainTextFormatter() {
    override val format = ILogManager.Format.PLAIN

    override fun writeLine(input: CharSequence, out: Writer) {
        out.appendln(
            (input as? OutputLine)?.toAttributedString()
                ?: stripAnsi(input)
        )
    }
}