package net.dhleong.judo.render

interface IJudoAppendable {
    /**
     * Append text to the last line
     */
    fun append(text: FlavorableCharSequence)

    /**
     * Append a whole line
     */
    fun appendLine(line: FlavorableCharSequence)
}

interface IJudoBuffer : IJudoAppendable {
    val id: Int
    val size: Int
    val lastIndex: Int

    operator fun get(index: Int): FlavorableCharSequence

    fun clear()
    fun replaceLastLine(result: FlavorableCharSequence)

    fun set(newContents: List<FlavorableCharSequence>)
}

interface IJudoWindow : IJudoAppendable {
    val id: Int
    val width: Int
    val height: Int

    /**
     * Depending on the rendered representation, the
     * *visible* height may be less than the measured [height],
     * for example if a status bar takes up a line
     */
    val visibleHeight: Int

    val isFocusable: Boolean
    var isFocused: Boolean

    var currentBuffer: IJudoBuffer

    /** Must be -1 when cursor is not focused on status line */
    val statusCursor: Int

    /**
     * Optional property; if set, *when this window is focused* any user input
     * will come to this function instead of [net.dhleong.judo.IJudoCore.send].
     */
    var onSubmitFn: ((String) -> Unit)?

    /**
     * Common-use convenience
     */
    fun appendLine(line: String)

    fun resize(width: Int, height: Int)

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
    fun scrollBySetting(count: Int)
    fun scrollToBottom()

    /**
     * Scroll such that the given line in this window's [IJudoBuffer] is visible
     */
    fun scrollToBufferLine(line: Int, offsetOnLine: Int = 0)

    fun updateStatusLine(line: FlavorableCharSequence, cursor: Int = -1)
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

    fun resize(width: Int, height: Int)

    fun getYPositionOf(window: IJudoWindow): Int

    fun unsplit()
    fun close(window: IJudoWindow)
}
