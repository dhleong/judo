package net.dhleong.judo.render

import net.dhleong.judo.StateMap

/**
 * Core, non-rendering-dependent [IJudoWindow] implementations
 *
 * @author dhleong
 */
abstract class BaseJudoWindow(
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

    protected var lastSearchKeyword: String = ""
    protected var searchResultLine = -1
    protected var searchResultOffset = -1

    override fun append(text: FlavorableCharSequence) = currentBuffer.append(text)
    override fun appendLine(line: FlavorableCharSequence) = currentBuffer.appendLine(line)
    override fun appendLine(line: String) = appendLine(
        FlavorableStringBuilder.withDefaultFlavor(line)
    )

    override fun scrollPages(count: Int) {
        scrollLines(height * count)
    }

    override fun searchForKeyword(word: CharSequence, direction: Int) {
        val ignoreCase = true // TODO smartcase setting?

        val originalSearchResultLine = searchResultLine
        val originalSearchResultOffset = searchResultOffset
        val wordString = word.toString()
        if (wordString != lastSearchKeyword) {
            lastSearchKeyword = wordString
            searchResultLine = -1
            searchResultOffset = -1
        }

        val buffer = currentBuffer
        val searchRange = when {
            direction > 0 -> (buffer.size - getScrollback() - 1) downTo 0

            else -> getScrollback() until buffer.size
        }

        for (i in searchRange) {
            val line = buffer[i]
            val continueSearchOnLine = i == searchResultLine
            val index = when {
                direction > 0 -> line.lastIndexOf(
                    wordString,
                    ignoreCase = ignoreCase,
                    startIndex = when {
                        continueSearchOnLine -> searchResultOffset - 1
                        else -> line.length
                    }
                )

                else -> line.indexOf(
                    wordString,
                    ignoreCase = ignoreCase,
                    startIndex = when {
                        continueSearchOnLine -> searchResultOffset + 1
                        else -> 0
                    }
                )
            }

            if (index >= 0) {
                searchResultLine = i
                searchResultOffset = index
                scrollToBufferLine(i, offsetOnLine = index)
                return
            }
        }

        // couldn't find anything; reset
        if (originalSearchResultLine != -1) {
            searchResultLine = originalSearchResultLine
            searchResultOffset = originalSearchResultOffset
        }

        // TODO bell? echo?
        buffer.appendLine(FlavorableStringBuilder.withDefaultFlavor(
            "Pattern not found: $word"
        ))
    }

}