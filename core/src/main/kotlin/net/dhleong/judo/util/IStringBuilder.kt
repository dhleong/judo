package net.dhleong.judo.util

import org.jline.utils.AttributedCharSequence

/**
 * @author dhleong
 */

interface IStringBuilder : CharSequence, Appendable {
    companion object {
        val EMPTY: IStringBuilder = from("")

        fun create(capacity: Int): IStringBuilder =
            ReplaceableAttributedStringBuilder(capacity)

        fun from(actual: CharSequence) =
            when (actual) {
                is IStringBuilder -> actual
                is AttributedCharSequence -> {
                    val result = ReplaceableAttributedStringBuilder(actual.length)
                    result.append(actual)
                    result
                }
                else -> ReplaceableAttributedStringBuilder(actual.toString())
            }
    }

    fun replace(start: Int, end: Int, str: String)

    fun setLength(newLength: Int)

    fun toAnsiString(): String = toString()
}

