package net.dhleong.judo.complete

import net.dhleong.judo.input.InputBuffer

/**
 * @author dhleong
 */
class CompletionSuggester(private val completions: CompletionSource) {
    private var pendingSuggestions: Iterator<String>? = null
    private val suggestionsStack = ArrayList<String>()

    /** Points to the next index in the stack that next() should read */
    private var suggestionsStackIndex = 0

    private var suggestedWordStart = 0
    private lateinit var originalWord: CharSequence

    fun reset() {
        pendingSuggestions = null
        suggestionsStack.clear()
        suggestionsStackIndex = 0
    }

    fun isInitialized(): Boolean =
        pendingSuggestions != null

    fun initialize(input: CharSequence, cursor: Int) {
        var wordStart = cursor
        while (wordStart >= 0
            && (wordStart >= input.length
                || !Character.isWhitespace(input[wordStart]))) {
            --wordStart
        }

        ++wordStart
        suggestedWordStart = wordStart

        val word = input.subSequence(wordStart until cursor)
        originalWord = word
        pendingSuggestions = completions.suggest(word).iterator()
    }

    /** NOTE: ASSUMES that initialize() was called */
    fun updateWithNextSuggestion(buffer: InputBuffer) {
        applySuggestion(buffer, nextSuggestion())
    }

    fun updateWithPrevSuggestion(buffer: InputBuffer) {
        applySuggestion(buffer, prevSuggestion())
    }

    private fun applySuggestion(buffer: InputBuffer, suggestion: CharSequence) {
        val oldWordRange = suggestedWordStart..buffer.cursor
        buffer.replace(oldWordRange, suggestion)
        buffer.cursor = suggestedWordStart + suggestion.length
    }

    /** NOTE: ASSUMES that initialize() was called */
    fun nextSuggestion(): CharSequence {
        if (suggestionsStackIndex >= 0
                && suggestionsStackIndex < suggestionsStack.size) {
            return suggestionsStack[suggestionsStackIndex++]
        }

        val iter = pendingSuggestions!!
        if (!iter.hasNext() && suggestionsStack.isNotEmpty()) {
            return suggestionsStack.last()
        } else if (!iter.hasNext()) {
            // no more suggestions, and no stack. we never had any
            return originalWord
        }

        val newSuggestion = iter.next()
        suggestionsStack.add(newSuggestion)
        ++suggestionsStackIndex
        return newSuggestion
    }

    fun prevSuggestion(): CharSequence {
        suggestionsStackIndex = maxOf(0, suggestionsStackIndex - 1)
        val toRead = suggestionsStackIndex - 1
        if (toRead < 0) return originalWord
        return suggestionsStack[toRead]
    }
}
