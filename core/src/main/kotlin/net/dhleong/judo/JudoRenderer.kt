package net.dhleong.judo

import java.io.Closeable

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

    fun validate()

    fun appendOutputLine(line: String)
    fun appendOutput(buffer: CharArray, count: Int)

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