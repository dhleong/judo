package net.dhleong.judo.register

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

/**
 * @author dhleong
 */
class ClipboardRegister : IRegister {
    override var value: CharSequence
        get() = try {
            clipboard.getData(DataFlavor.stringFlavor).let {
                cleanClipboardContents(it.toString())
            }
        } catch (e: Exception) {
            when (e) {
                // these three might be thrown from getData;
                // they all mean we couldn't get appropriate
                // clipboard data (IE: text); so just return ""
                is UnsupportedFlavorException,
                is IllegalStateException,
                is IOException -> ""

                else -> throw e
            }
        }
        set(value) {
            val selection = StringSelection(value.toString())
            clipboard.setContents(selection, selection)
        }

    private val clipboard = Toolkit.getDefaultToolkit().systemClipboard

    // strip out newlines
    private fun cleanClipboardContents(text: String): String =
        text.replace(Regex("[\r\n]"), "")
}

class SimpleRegister : IRegister {
    override var value: CharSequence
        get() = buffer
        set(value) {
            buffer.setLength(0)
            buffer.append(value)
        }

    override fun copyFrom(sequence: CharSequence, start: Int, end: Int) {
        buffer.setLength(0)
        buffer.append(sequence, start, end)
    }

    private val buffer = StringBuilder()
}
