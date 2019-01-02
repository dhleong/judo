package net.dhleong.judo.render

import net.dhleong.judo.StateMap

/**
 * Special window that delegates to two separate windows, one
 * containing the primary output buffer for a Connection, and
 * one containing prompts extracted for that Connection.
 */
abstract class PrimaryJudoWindow2(
    ids: IdManager,
    settings: StateMap,
    outputBuffer: IJudoBuffer2,
    initialWidth: Int,
    initialHeight: Int
) : IJudoWindow2 {

    override val id = ids.newWindow()
    override var width = initialWidth
    override var height = initialHeight
    override var isFocused: Boolean
        get() = promptWindow.isFocused
        set(value) {
            promptWindow.isFocused = value
        }
    override val isFocusable = true // primary window is ALWAYS focusable

    override var currentBuffer: IJudoBuffer2
        get() = outputWindow.currentBuffer
        set(value) {
            outputWindow.currentBuffer = value
        }

    override val statusCursor: Int
        get() = promptWindow.statusCursor

    @Suppress("LeakingThis")
    val outputWindow = createWindow(ids, settings,
        initialWidth, initialHeight - 1,
        outputBuffer,
        isFocusable = false
    )

    @Suppress("LeakingThis")
    val promptBuffer = createBuffer(ids)

    @Suppress("LeakingThis")
    val promptWindow = createWindow(ids, settings,
        initialWidth, 1,
        promptBuffer,
        isFocusable = true,
        statusLineOverlaysOutput = true
    )

    abstract fun createBuffer(ids: IdManager): IJudoBuffer2

    abstract fun createWindow(
        ids: IdManager,
        settings: StateMap,
        initialWidth: Int,
        initialHeight: Int,
        initialBuffer: IJudoBuffer2,
        isFocusable: Boolean = false,
        statusLineOverlaysOutput: Boolean = false
    ): IJudoWindow2

    override fun updateStatusLine(line: CharSequence, cursor: Int) =
        promptWindow.updateStatusLine(line, cursor)

    override fun resize(width: Int, height: Int) {
        val availableHeight = height - promptWindow.height
        outputWindow.resize(width, availableHeight)
        this.width = width
        this.height = height
    }

    fun setPromptHeight(promptHeight: Int) {
        promptWindow.resize(width, maxOf(1, promptHeight))
        resize(width, height)
    }

    override fun getScrollback(): Int = outputWindow.getScrollback()
    override fun scrollLines(count: Int) = outputWindow.scrollLines(count)
    override fun scrollPages(count: Int) = outputWindow.scrollPages(count)
    override fun scrollToBottom() = outputWindow.scrollToBottom()
    override fun scrollToBufferLine(line: Int, offsetOnLine: Int) =
        outputWindow.scrollToBufferLine(line, offsetOnLine)
    override fun searchForKeyword(word: CharSequence, direction: Int) =
        outputWindow.searchForKeyword(word, direction)
}
