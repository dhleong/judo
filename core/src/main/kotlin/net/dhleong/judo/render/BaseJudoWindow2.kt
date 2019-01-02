package net.dhleong.judo.render

import net.dhleong.judo.StateMap

/**
 * Core, non-rendering-dependent [IJudoWindow2] implementations
 *
 * @author dhleong
 */
abstract class BaseJudoWindow2(
    ids: IdManager,
    protected val settings: StateMap,
    initialWidth: Int,
    initialHeight: Int,
    initialBuffer: IJudoBuffer2,
    override val isFocusable: Boolean = false,
    val statusLineOverlaysOutput: Boolean = false
) : IJudoWindow2 {

    override val id = ids.newWindow()
    override var width: Int = initialWidth
    override var height: Int = initialHeight

    protected var lastSearchKeyword: String = ""
    protected var searchResultLine = -1

    override fun scrollPages(count: Int) {
        scrollLines(height * count)
    }

    override fun searchForKeyword(word: CharSequence, direction: Int) {
        val ignoreCase = true // TODO smartcase setting?

        val originalSearchResultLine = searchResultLine
        val wordString = word.toString()
        if (wordString != lastSearchKeyword) {
            lastSearchKeyword = wordString
            searchResultLine = -1
        }

        val buffer = currentBuffer
        val searchRange = when {
            direction > 0 -> (buffer.size - getScrollback() - 1) downTo 0

            else -> getScrollback() until buffer.size
        }

        for (i in searchRange) {
            val index = buffer[i].indexOf(wordString, ignoreCase = ignoreCase)
            if (index >= 0) {
                searchResultLine = i
                scrollToBufferLine(i, offsetOnLine = index)
                return
            }
        }

        // couldn't find anything; reset
        if (originalSearchResultLine != -1) {
            searchResultLine = originalSearchResultLine
        }

        // TODO bell? echo?
        buffer.appendLine(FlavorableStringBuilder.withDefaultFlavor(
            "Pattern not found: $word"
        ))
    }

}