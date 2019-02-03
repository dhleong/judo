package net.dhleong.judo.modes

import net.dhleong.judo.Mode
import net.dhleong.judo.render.FlavorableCharSequence

/**
 * An InputBufferProvider is some sort of Mode
 *  that renders something in the input bar
 */
interface InputBufferProvider : Mode {

    fun renderInputBuffer(): FlavorableCharSequence
    fun getCursor(): Int

}