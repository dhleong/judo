package net.dhleong.judo.complete

import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.motions.wordMotion

internal interface CapitalizationStrategy {
    companion object {
        fun detect(original: CharSequence): CapitalizationStrategy {
            if (original.isEmpty()) return AsIsCapitalization()

            val capitals = original.filter { Character.isUpperCase(it) }.sumBy { 1 }
            return when (capitals) {
                1 -> TitleCaseCapitalization()
                original.length -> AllCapsCapitalization()
                else -> AsIsCapitalization()
            }
        }
    }

    fun capitalize(word: CharSequence): CharSequence
}

class AllCapsCapitalization : CapitalizationStrategy {
    override fun capitalize(word: CharSequence): CharSequence =
        word.toString().toUpperCase()
}

class AsIsCapitalization : CapitalizationStrategy {
    override fun capitalize(word: CharSequence): CharSequence = word
}

class TitleCaseCapitalization : CapitalizationStrategy {
    override fun capitalize(word: CharSequence): CharSequence {
        if (Character.isUpperCase(word[0])) return word

        val upperFirst = Character.toUpperCase(word[0])
        return "$upperFirst${word.subSequence(1, word.length)}"
    }
}

/**
 * @author dhleong
 */
class CompletionSuggester(private val completions: CompletionSource) {
    private val wordStartMovement = wordMotion(-1, bigWord = false)

    private var pendingSuggestions: Iterator<String>? = null
    private val suggestionsStack = ArrayList<String>()

    /** Points to the next index in the stack that next() should read */
    private var suggestionsStackIndex = 0

    private var suggestedWordStart = 0
    private lateinit var originalWord: CharSequence
    private lateinit var capitalizationStrategy: CapitalizationStrategy

    fun reset() {
        pendingSuggestions = null
        suggestionsStack.clear()
        suggestionsStackIndex = 0
    }

    fun isInitialized(): Boolean =
        pendingSuggestions != null

    fun initialize(input: CharSequence, cursor: Int) {
        val move = wordStartMovement.calculate(input, cursor)
        val wordStart = move.endInclusive

        val wordRange: IntRange
        if (wordStart < 0) {
            wordRange = 0 until cursor
            suggestedWordStart = 0
        } else {
            suggestedWordStart = wordStart
            wordRange = wordStart until cursor
        }
        val word = input.subSequence(wordRange)
        originalWord = word
        capitalizationStrategy = CapitalizationStrategy.detect(word)
        pendingSuggestions = completions.suggest(input, wordRange).iterator()
    }

    /** NOTE: ASSUMES that initialize() was called */
    fun updateWithNextSuggestion(buffer: InputBuffer) {
        applySuggestion(buffer, nextSuggestion())
    }

    fun updateWithPrevSuggestion(buffer: InputBuffer) {
        applySuggestion(buffer, prevSuggestion())
    }

    private fun applySuggestion(buffer: InputBuffer, suggestion: CharSequence) {
        val oldWordRange = suggestedWordStart until buffer.cursor
        buffer.replace(oldWordRange, capitalizationStrategy.capitalize(suggestion))
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
