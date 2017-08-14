package net.dhleong.judo.register

import net.dhleong.judo.util.ClipboardFacade

/**
 * The black hole register `_` ignores any set attempts,
 *  and always contains only the empty string
 *
 * @author dhleong
 */
class BlackHoleRegister : IRegister {
    override var value: CharSequence
        get() = ""
        set(value) {}

    override fun copyFrom(sequence: CharSequence, start: Int, end: Int) {
        // nop
    }
}

/**
 * Reads to and writes from the system clipboard.
 *
 * @author dhleong
 */
class ClipboardRegister : IRegister {
    override var value: CharSequence
        get() = cleanClipboardContents(clipboard.read())
        set(value) = clipboard.write(value.toString())

    private val clipboard: ClipboardFacade by lazy { ClipboardFacade.newInstance() }

    // strip out newlines
    private fun cleanClipboardContents(text: String): String =
        text.replace(Regex("[\r\n]"), "")
}

/**
 * Your standard, garden-variety, in-memory register
 */
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
