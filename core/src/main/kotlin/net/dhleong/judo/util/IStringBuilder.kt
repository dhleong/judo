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
//                is StringBuilder -> DelegateIStringBuilder(actual)
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

internal class DelegateIStringBuilder(val delegate: StringBuilder)
        : IStringBuilder,
            Appendable by delegate,
            CharSequence by delegate {

    override fun replace(start: Int, end: Int, str: String) {
        delegate.replace(start, end, str)
    }

    override fun setLength(newLength: Int) {
        delegate.setLength(newLength)
    }

    override fun toString(): String =
        delegate.toString()
}
