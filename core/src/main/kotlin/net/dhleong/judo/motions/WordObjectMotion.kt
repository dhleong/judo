package net.dhleong.judo.motions

/**
 * @author dhleong
 */

fun innerWordObjectMotion(bigWord: Boolean): Motion {
    if (!bigWord) return innerWordObjectMotion()

    val isWordBoundary = wordBoundaryFor(bigWord)
    val startMotion = wordMotion(-1, bigWord)
    val endMotion = endOfWordMotion(1, bigWord)

    return createMotion(Motion.Flags.TEXT_OBJECT) { buffer, cursor ->
        val start: Int
        if (cursor > 0 && !isWordBoundary(buffer[cursor - 1])) {
            start = startMotion.calculate(buffer, cursor).endInclusive
        } else {
            start = cursor
        }

        var end = endMotion.calculate(buffer, start).endInclusive
        if (end <= buffer.lastIndex && !isWordBoundary(buffer[end])) {
            ++end
        }

        start..end
    }
}

private enum class WordType {
    IDENTIFIER { override fun matches(char: Char) = Character.isJavaIdentifierPart(char) && char != '$' },
    WHITESPACE { override fun matches(char: Char) = Character.isWhitespace(char) },
    SYMBOL { override fun matches(char: Char) = !(IDENTIFIER.matches(char) || WHITESPACE.matches(char)) };

    abstract fun matches(char: Char): Boolean

    companion object {
        fun of(char: Char): WordType = when {
            IDENTIFIER.matches(char) -> IDENTIFIER
            WHITESPACE.matches(char) -> WHITESPACE
            else -> SYMBOL
        }
    }
}

private fun innerWordObjectMotion(): Motion =
    createMotion(Motion.Flags.TEXT_OBJECT) { buffer, cursor ->
        if (buffer.isEmpty()) {
            // special case
            cursor..cursor
        } else {
            val initial = buffer[cursor]
            val initialType = WordType.of(initial)

            var start = cursor
            while (start > 0 && initialType.matches(buffer[start - 1])) {
                --start
            }

            var end = cursor
            val last = buffer.lastIndex
            while (end <= last && initialType.matches(buffer[end])) {
                ++end
            }

            start..end
        }
    }

fun outerWordObjectMotion(bigWord: Boolean): Motion {
    val inner = innerWordObjectMotion(bigWord)
    return createMotion(Motion.Flags.TEXT_OBJECT) { buffer, cursor ->
        if (buffer.isEmpty()) {
            // special case
            cursor..cursor
        } else {
            val word = inner.calculate(buffer, cursor)
            var start = word.start
            var end = word.endInclusive

            if (end < buffer.length && Character.isWhitespace(buffer[end])) {
                // try to expand right into whitespace
                while (end < buffer.length && Character.isWhitespace(buffer[end])) {
                    ++end
                }

            } else {
                // we're on a word boundary; expand left instead
                while (start > 0 && Character.isWhitespace(buffer[start - 1])) {
                    --start
                }
            }

            start..end
        }
    }
}