package net.dhleong.judo.logging

import java.io.Writer

/**
 * @author dhleong
 */
interface ILogFormatter {

    val format: ILogManager.Format

    fun writeHeader(out: Writer)
    fun writeLine(input: CharSequence, out: Writer)
    fun writeFooter(out: Writer)

}
