package net.dhleong.judo.render

/**
 * @author dhleong
 */
interface IJudoBuffer {
    val id: Int
    val isFocusable: Boolean
    val size: Int
    val lastIndex: Int

    operator fun get(index: Int): CharSequence

    fun appendLine(
        line: CharSequence, isPartialLine: Boolean,
        windowWidthHint: Int, wordWrap: Boolean
    ): CharSequence

    fun clear()
    fun replaceLastLine(result: CharSequence)
}

interface InputReceivingBuffer : IJudoBuffer {
    fun send(line: CharSequence)
}

interface IJudoWindow {
    val id: Int
    val width: Int
    val height: Int
    val isFocusable: Boolean get() = currentBuffer.isFocusable
    var isFocused: Boolean

    var currentBuffer: IJudoBuffer

    fun resize(width: Int, height: Int)

    fun getDisplayLines(lines: MutableList<CharSequence>)
    fun appendLine(line: CharSequence, isPartialLine: Boolean): CharSequence

    fun updateStatusLine(line: CharSequence, cursor: Int = -1)
}

interface IJudoTabpage {
    val id: Int
    val width: Int
    val height: Int

    var currentWindow: IJudoWindow

    fun vsplit(percentage: Float, buffer: IJudoBuffer): IJudoWindow
    fun vsplit(cols: Int, buffer: IJudoBuffer): IJudoWindow

    fun hsplit(percentage: Float, buffer: IJudoBuffer): IJudoWindow
    fun hsplit(cols: Int, buffer: IJudoBuffer): IJudoWindow

    fun resize(width: Int, height: Int)

    fun getDisplayLines(lines: MutableList<CharSequence>)
}

class IdManager {
    private var nextBufferId = 0
    private var nextTabpageId = 0
    private var nextWindowId = 0

    fun newBuffer() = ++nextBufferId
    fun newTabpage() = ++nextTabpageId
    fun newWindow() = ++nextWindowId
}