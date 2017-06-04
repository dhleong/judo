package net.dhleong.judo.util

import org.jline.utils.AttributedCharSequence

/**
 * @author dhleong
 */

interface IStringBuilder : CharSequence {
    companion object {
        fun from(actual: CharSequence) =
            when (actual) {
                is IStringBuilder -> actual
                is StringBuilder -> DelegateIStringBuilder(actual)
                is AttributedCharSequence -> {
                    val result = ReplaceableAttributedStringBuilder(actual.length)
                    result.append(actual)
                    result
                }
                else -> DelegateIStringBuilder(StringBuilder(actual))
            }
    }

    fun replace(start: Int, end: Int, str: String)

}

internal class DelegateIStringBuilder(val delegate: StringBuilder) : IStringBuilder {
    override val length: Int
        get() = delegate.length

    override fun get(index: Int): Char =
        delegate[index]

    override fun replace(start: Int, end: Int, str: String) {
        delegate.replace(start, end, str)
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        delegate.subSequence(startIndex, endIndex)

    override fun toString(): String =
        delegate.toString()
}
