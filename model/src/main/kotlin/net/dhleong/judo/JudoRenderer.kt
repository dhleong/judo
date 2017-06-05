package net.dhleong.judo

import java.io.Closeable

typealias OnResizedEvent = () -> Unit

/**
 * @author dhleong
 */
interface JudoRenderer : Closeable {
    /**
     * The type of terminal this is rendering to
     * (or is emulating), eg VT100
     */
    val terminalType: String

    val windowHeight: Int
    val windowWidth: Int

    /**
     * Fired if the [windowWidth] or [windowHeight]
     *  values changed; run inside a transaction
     */
    var onResized: OnResizedEvent?

    /**
     * Make sure this renderer can be used
     */
    fun validate()

    /**
     * @param isPartialLine If true, the next call to this function
     * should append its value to this line instead of adding it
     * as a new line
     */
    fun appendOutput(line: CharSequence, isPartialLine: Boolean = false)

    fun inTransaction(block: () -> Unit)

    /**
     * Current lines scrolled back
     */
    fun getScrollback(): Int

    /**
     * @param count Number of lines to scroll, where a POSITIVE number
     *  moves backward in history, and a NEGATIVE number moves forward
     */
    fun scrollLines(count: Int)

    /**
     * @see scrollLines
     */
    fun scrollPages(count: Int)
    fun scrollToBottom()

    fun updateInputLine(line: String, cursor: Int)

    fun updateStatusLine(line: String, cursor: Int = -1)
}