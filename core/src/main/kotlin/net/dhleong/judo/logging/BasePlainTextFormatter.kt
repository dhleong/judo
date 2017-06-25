package net.dhleong.judo.logging

import java.io.Writer

/**
 * @author dhleong
 */
abstract class BasePlainTextFormatter : ILogFormatter {

    override fun writeHeader(out: Writer) {
        // TODO write the date or something?
    }

    override fun writeFooter(out: Writer) {
        // nop
    }

}