package net.dhleong.judo.input

/**
 * @author dhleong
 */
interface IBufferWithCursor {
    var cursor: Int
    val size: Int
    val lastIndex: Int get() = size - 1
    fun toChars(): CharSequence
    fun get(range: IntRange): CharSequence
    fun clear()
}