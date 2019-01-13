package net.dhleong.judo.search

import net.dhleong.judo.render.IJudoBuffer

/**
 * @author dhleong
 */
class BufferSearcher {

    val hasResult: Boolean
        get() = searchResultLine >= 0

    /**
     * Line number (index) in the last buffer
     */
    val resultLine: Int
        get() = searchResultLine

    /**
     * Offset into [resultLine]
     */
    val resultOffset: Int
        get() = searchResultOffset

    val lastKeyword: String
        get() = lastSearchKeyword

    private var lastSearchKeyword: String = ""
    private var searchResultLine = -1
    private var searchResultOffset = -1

    fun reset() {
        searchResultLine = -1
        searchResultOffset = -1
    }

    fun searchForKeyword(
        buffer: IJudoBuffer,
        windowScrollback: Int,
        word: CharSequence,
        direction: Int,
        ignoreCase: Boolean
    ): Boolean {

        val originalSearchResultLine = searchResultLine
        val originalSearchResultOffset = searchResultOffset
        val wordString = word.toString()
        if (wordString != lastSearchKeyword) {
            lastSearchKeyword = wordString
            reset()
        }

        val searchRange = when {
            direction > 0 -> (buffer.lastIndex - windowScrollback) downTo 0

            else -> windowScrollback until buffer.size
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
                return true
            }
        }

        // couldn't find anything; reset
        if (originalSearchResultLine != -1) {
            searchResultLine = originalSearchResultLine
            searchResultOffset = originalSearchResultOffset
        }

        return false
    }
}