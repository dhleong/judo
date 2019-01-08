package net.dhleong.judo.modes

import net.dhleong.judo.render.FlavorableCharSequence

/**
 * @author dhleong
 */

interface StatusBufferProvider {

    fun renderStatusBuffer(): FlavorableCharSequence
    fun getCursor(): Int

}
