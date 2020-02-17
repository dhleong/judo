package net.dhleong.judo.input

/**
 * @author dhleong
 */
interface IBufferWithCursor {
    var cursor: Int
    val size: Int
    fun toChars(): CharSequence
}