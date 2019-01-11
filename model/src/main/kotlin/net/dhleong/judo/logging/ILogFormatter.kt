package net.dhleong.judo.logging

import net.dhleong.judo.render.FlavorableCharSequence
import java.io.Writer

/**
 * @author dhleong
 */
interface ILogFormatter {

    val format: ILogManager.Format

    fun writeHeader(out: Writer)
    fun writeLine(input: FlavorableCharSequence, out: Writer)
    fun writeFooter(out: Writer)

}
