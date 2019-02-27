package net.dhleong.judo.render

import net.dhleong.judo.StateMap

/**
 * Special window that delegates to two separate windows, one
 * containing the primary output buffer for a Connection, and
 * one containing prompts extracted for that Connection.
 */
abstract class PrimaryJudoWindow(
    ids: IdManager,
    settings: StateMap,
    outputBuffer: IJudoBuffer,
    initialWidth: Int,
    initialHeight: Int
) : IJudoWindow {

    override val id = ids.newWindow()
    override var width = initialWidth
    override var height = initialHeight
    override val visibleHeight: Int
        get() = height
    override var isFocused: Boolean
        get() = promptWindow.isFocused
        set(value) {
            promptWindow.isFocused = value
        }
    override val isFocusable = true // primary window is ALWAYS focusable

    override var currentBuffer: IJudoBuffer
        get() = outputWindow.currentBuffer
        set(value) {
            outputWindow.currentBuffer = value
        }

    override val statusCursor: Int
        get() = promptWindow.statusCursor

    override var onSubmitFn: ((String) -> Unit)?
        get() = null
        set(_) {
            throw IllegalArgumentException(
                "You may not override onSubmit of a Primary window"
            )
        }

    // have to create lazy since the implementing class may depend
    // on fields initialized by its constructor.
    // NOTE: these should be init'd pretty quickly by the first render,
    // so we shouldn't have to add synchronization overhead to the lazy
    val outputWindow by lazy(LazyThreadSafetyMode.NONE) {
        createWindow(
            ids, settings,
            initialWidth, initialHeight - 1,
            outputBuffer,
            isFocusable = false
        )
    }

    private val promptBuffer by lazy(LazyThreadSafetyMode.NONE) {
        createBuffer(ids)
    }

    val promptWindow by lazy(LazyThreadSafetyMode.NONE) {
        createWindow(
            ids, settings,
            initialWidth, 1,
            promptBuffer,
            isFocusable = true,
            statusLineOverlaysOutput = true
        )
    }

    private var promptHeight = 1

    abstract fun createBuffer(ids: IdManager): IJudoBuffer

    abstract fun createWindow(
        ids: IdManager,
        settings: StateMap,
        initialWidth: Int,
        initialHeight: Int,
        initialBuffer: IJudoBuffer,
        isFocusable: Boolean = false,
        statusLineOverlaysOutput: Boolean = false
    ): IJudoWindow

    override fun append(text: FlavorableCharSequence) = outputWindow.append(text)
    override fun appendLine(line: FlavorableCharSequence) = outputWindow.appendLine(line)
    override fun appendLine(line: String) = outputWindow.appendLine(line)

    override fun updateStatusLine(line: FlavorableCharSequence, cursor: Int) =
        promptWindow.updateStatusLine(line, cursor)

    override fun resize(width: Int, height: Int) {
        promptWindow.resize(width, promptHeight)

        val availableHeight = height - promptHeight
        outputWindow.resize(width, availableHeight)
        this.width = width
        this.height = height
    }

    fun setPromptHeight(promptHeight: Int) {
        this.promptHeight = maxOf(1, promptHeight)
        resize(width, height)
    }

    override fun getScrollback(): Int = outputWindow.getScrollback()
    override fun scrollLines(count: Int) = outputWindow.scrollLines(count)
    override fun scrollPages(count: Int) = outputWindow.scrollPages(count)
    override fun scrollBySetting(count: Int) = outputWindow.scrollBySetting(count)
    override fun scrollToBottom() = outputWindow.scrollToBottom()
    override fun scrollToBufferLine(line: Int, offsetOnLine: Int) =
        outputWindow.scrollToBufferLine(line, offsetOnLine)
    override fun searchForKeyword(word: CharSequence, direction: Int) =
        outputWindow.searchForKeyword(word, direction)
}
