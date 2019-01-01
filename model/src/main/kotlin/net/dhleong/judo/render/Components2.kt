package net.dhleong.judo.render

interface IJudoBuffer2 {
    val id: Int
    val size: Int
    val lastIndex: Int

    operator fun get(index: Int): FlavorableCharSequence

    /**
     * Append text to the last line
     */
    fun append(text: FlavorableCharSequence)

    /**
     * Append a whole line
     */
    fun appendLine(line: FlavorableCharSequence)

    fun clear()
    fun replaceLastLine(result: FlavorableCharSequence)

    fun set(newContents: List<FlavorableCharSequence>)
}

interface IJudoWindow2 {
    val id: Int
    val width: Int
    val height: Int
    val isFocusable: Boolean
    var isFocused: Boolean

    var currentBuffer: IJudoBuffer2

    /** Must be -1 when cursor is not focused on status line */
    val statusCursor: Int

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
    fun scrollToBottom()

    fun updateStatusLine(line: CharSequence, cursor: Int = -1)
    fun searchForKeyword(word: CharSequence, direction: Int)
}

interface IJudoTabpage2 {
    val id: Int
    val width: Int
    val height: Int

    var currentWindow: IJudoWindow2

    fun findWindowById(id: Int): IJudoWindow2?
    fun findWindowByBufferId(id: Int): IJudoWindow2?

    fun vsplit(percentage: Float, buffer: IJudoBuffer2): IJudoWindow2
    fun vsplit(cols: Int, buffer: IJudoBuffer2): IJudoWindow2

    fun hsplit(percentage: Float, buffer: IJudoBuffer2): IJudoWindow2
    fun hsplit(rows: Int, buffer: IJudoBuffer2): IJudoWindow2

    fun resize()
    fun resize(width: Int, height: Int)

    fun getYPositionOf(window: IJudoWindow2): Int

    fun unsplit()
    fun close(window: IJudoWindow2)
}
