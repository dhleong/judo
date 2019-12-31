package net.dhleong.judo.render

import net.dhleong.judo.JudoRenderer
import net.dhleong.judo.SCROLL
import net.dhleong.judo.StateMap
import net.dhleong.judo.search.BufferSearcher

/**
 * Core, non-rendering-dependent [IJudoWindow] implementations
 *
 * @author dhleong
 */
abstract class BaseJudoWindow(
    private val renderer: JudoRenderer,
    ids: IdManager,
    protected val settings: StateMap,
    initialWidth: Int,
    initialHeight: Int,
    override val isFocusable: Boolean = false,
    val statusLineOverlaysOutput: Boolean = false
) : IJudoWindow {

    override val id = ids.newWindow()
    override var width: Int = initialWidth
    override var height: Int = initialHeight

    override var isWindowHidden: Boolean = false

    protected val search = BufferSearcher()

    override var onSubmitFn: ((String) -> Unit)? = null

    override fun append(text: FlavorableCharSequence) = currentBuffer.append(text)
    override fun appendLine(line: FlavorableCharSequence) = currentBuffer.appendLine(line)
    override fun appendLine(line: String) = appendLine(
        FlavorableStringBuilder.withDefaultFlavor(line)
    )

    override fun scrollPages(count: Int) {
        scrollLines(visibleHeight * count)
    }

    override fun scrollBySetting(count: Int) {
        // NOTE: currently, count is only used for the direction
        val normalizedCount = count / kotlin.math.abs(count)
        
        val setting = settings[SCROLL]
        if (setting <= 0) {
            scrollLines((visibleHeight / 2) * normalizedCount)
        } else {
            scrollLines(setting * normalizedCount)
        }
    }

    override fun searchForKeyword(word: CharSequence, direction: Int) {
        val ignoreCase = true // TODO smartcase setting?

        val buffer = currentBuffer
        val found = search.searchForKeyword(
            buffer,
            getScrollback(),
            word,
            direction,
            ignoreCase
        )

        if (!found) {
            // TODO bell?
            renderer.echo("Pattern not found: $word".toFlavorable())
            return
        }

        scrollToBufferLine(search.resultLine, offsetOnLine = search.resultOffset)
    }

}