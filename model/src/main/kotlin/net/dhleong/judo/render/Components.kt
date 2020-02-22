package net.dhleong.judo.render

import net.dhleong.judo.IStateMap
import net.dhleong.judo.script.IJudoScrollable
import java.io.File

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
    val settings: IStateMap

    val id: Int
    val size: Int
    val lastIndex: Int

    operator fun get(index: Int): FlavorableCharSequence

    fun clear()
    fun deleteLast(): FlavorableCharSequence
    fun replaceLastLine(result: FlavorableCharSequence)

    fun set(newContents: List<FlavorableCharSequence>)
    operator fun set(index: Int, line: FlavorableCharSequence)

    fun setPersistent(file: File)
    fun setNotPersistent()

    fun attachWindow(window: IJudoWindow)
    fun detachWindow(window: IJudoWindow)
}

interface IJudoWindow : IJudoAppendable, IJudoScrollable {
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
    var isOutputFocused: Boolean

    /** counted from the *bottom* of the window */
    var cursorLine: Int
    var cursorCol: Int

    /**
     * "hidden" windows are not focusable and do not render (IE:
     * they effectively have 0 width and height). This allows us
     * to create the overall layout as intended *once* and just
     * hide windows that are not yet needed. This seems like a
     * common use case for MUDs; it's less interesting to be
     * constantly moving and resizing windows as you might when
     * editing text/code.
     */
    var isWindowHidden: Boolean

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
     * Scroll such that the given line in this window's [IJudoBuffer] is visible
     */
    fun scrollToBufferLine(line: Int, offsetOnLine: Int = 0)

    fun updateStatusLine(line: FlavorableCharSequence, cursor: Int = -1)
    fun searchForKeyword(word: CharSequence, direction: Int)

    fun computeCursorLocationInBuffer(): Pair<Int, Int>
    fun setCursorFromBufferLocation(line: Int, col: Int)

    /**
     * Called by the currentBuffer before a change is applied to it.
     * Useful if you need to do some prep for maintaining scroll position,
     * for example
     *
     * @return Any state object, passed back to you in [onBufModifyPost]
     */
    fun onBufModifyPre(): Any? = null
    fun onBufModifyPost(preState: Any?) {}
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
