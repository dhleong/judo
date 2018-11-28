package net.dhleong.judo.logging

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Writer

const val DATE_FORMAT = "yyyy MMMM dd  hh:mm a"

/**
 * @author dhleong
 */
class LogManager : ILogManager {

    var formatter: ILogFormatter? = null
    var output: Writer? = null

    override fun configure(destination: File, format: ILogManager.Format, mode: ILogManager.Mode) {
        val formatter = when (format) {
            ILogManager.Format.RAW -> RawLogFormatter()
            ILogManager.Format.HTML -> HtmlLogFormatter()
            else -> PlainLogFormatter()
        }

        val append = mode == ILogManager.Mode.APPEND
        val exists = destination.exists()
        val output = BufferedWriter(
            OutputStreamWriter(
                FileOutputStream(
                    destination,
                    append
                )
            )
        )

        if (!(exists && append)) {
            formatter.writeHeader(output)
        }

        this.formatter = formatter
        this.output = output
    }

    override fun unconfigure() {
        output?.let { out ->
            formatter?.apply {
                writeFooter(out)
            }

            out.flush()
            out.close()
        }

        formatter = null
        output = null
    }

    override fun log(rawOutputLine: CharSequence) {
        output?.let { out ->
            formatter?.writeLine(rawOutputLine, out)
        }
    }

}