package net.dhleong.judo.register

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/**
 * @author dhleong
 */
class ClipboardRegister : IRegister {
    override var value: CharSequence
        get() = clipboard.getContents(this).let {
            it.getTransferData(
                DataFlavor.selectBestTextFlavor(
                    it.transferDataFlavors
                )
            ).toString()
        }
        set(value) {
            val selection = StringSelection(value.toString())
            clipboard.setContents(selection, selection)
        }

    private val clipboard = Toolkit.getDefaultToolkit().systemClipboard
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
