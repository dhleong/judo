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

    fun updateInputLine(line: String, cursor: Int)

    fun updateStatusLine(line: String, cursor: Int = -1)
}