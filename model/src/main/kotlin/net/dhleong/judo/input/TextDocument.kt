package net.dhleong.judo.input

/**
 * @author dhleong
 */
interface TextDocument {
    var cursor: Int

    operator fun get(charOffset: Int): Char
    operator fun get(startOffset: Int, endOffset: Int): CharSequence

    val length: Int
    fun isEmpty(): Boolean
    fun startOfLine(charOffset: Int = cursor): Int
    fun endOfLine(charOffset: Int = cursor): Int
    fun lineIndex(charOffset: Int = cursor): Int
    fun linesCount(): Int

    fun clear()
    fun deleteRange(startOffset: Int, endOffset: Int): CharSequence
    fun insert(text: CharSequence, charOffset: Int = cursor)
    fun set(contents: CharSequence)
}

fun TextDocument.typeAtCursor(char: Char) {
    insert(char.toString(), charOffset = cursor)
    ++cursor
}
fun TextDocument.backspace() {
    deleteRange(cursor - 1, cursor)
    --cursor
}