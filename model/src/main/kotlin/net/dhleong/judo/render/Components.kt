package net.dhleong.judo.render

/**
 * @author dhleong
 */
interface IJudoBuffer {
    val id: Int
    val size: Int
    val lastIndex: Int

    operator fun get(index: Int): CharSequence

    fun appendLine(
        line: CharSequence, isPartialLine: Boolean,
        windowWidthHint: Int, wordWrap: Boolean
    ): CharSequence

    fun clear()
    fun replaceLastLine(result: CharSequence)

    fun set(newContents: List<CharSequence>)
}

interface InputReceivingBuffer : IJudoBuffer {
    fun send(line: CharSequence)
}

interface IJudoWindow {
    val id: Int
    val width: Int
    val height: Int
    val isFocusable: Boolean
    var isFocused: Boolean

    var currentBuffer: IJudoBuffer

    /** Must be -1 when cursor is not focused on status line */
    val statusCursor: Int

    fun appendLine(line: CharSequence, isPartialLine: Boolean): CharSequence

    fun getDisplayLines(lines: MutableList<CharSequence>)
    fun getScrollback(): Int

    fun resize(width: Int, height: Int)

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

    fun updateStatusLine(line: CharSequence, cursor: Int = -1)
    fun searchForKeyword(word: CharSequence, direction: Int)
}

interface IJudoTabpage {
    val id: Int
    val width: Int
    val height: Int

    var currentWindow: IJudoWindow

    fun findWindowById(id: Int): IJudoWindow?
    fun findWindowByBufferId(id: Int): IJudoWindow?

    fun vsplit(percentage: Float, buffer: IJudoBuffer): IJudoWindow
    fun vsplit(cols: Int, buffer: IJudoBuffer): IJudoWindow

    fun hsplit(percentage: Float, buffer: IJudoBuffer): IJudoWindow
    fun hsplit(rows: Int, buffer: IJudoBuffer): IJudoWindow

    fun resize()
    fun resize(width: Int, height: Int)

    fun getDisplayLines(lines: MutableList<CharSequence>)
    fun getYPositionOf(window: IJudoWindow): Int

    fun unsplit()
    fun close(window: IJudoWindow)
}

class IdManager {
    private var nextBufferId = 0
    private var nextTabpageId = 0
    private var nextWindowId = 0

    fun newBuffer() = ++nextBufferId
    fun newTabpage() = ++nextTabpageId
    fun newWindow() = ++nextWindowId
}