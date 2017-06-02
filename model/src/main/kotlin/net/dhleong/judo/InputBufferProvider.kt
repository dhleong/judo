package net.dhleong.judo

/**
 * An InputBufferProvider is some sort of Mode
 *  that renders something in the input bar
 */
interface InputBufferProvider : Mode {

    fun renderInputBuffer(): String
    fun getCursor(): Int

}