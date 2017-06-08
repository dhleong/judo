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

    fun isDiscardable(): Boolean = true

    fun replace(start: Int, end: Int, str: String)

    fun setLength(newLength: Int)

    // this method is hacks to get around AttributedCharSequence
    // changing the return type of subSequence to AttributedString
    fun slice(startIndex: Int, endIndex: Int): CharSequence =
        subSequence(startIndex, endIndex)

    fun toAnsiString(): String = toString()
}

