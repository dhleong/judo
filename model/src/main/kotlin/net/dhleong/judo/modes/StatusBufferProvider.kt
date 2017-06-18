package net.dhleong.judo.modes

/**
 * @author dhleong
 */

interface StatusBufferProvider {

    fun renderStatusBuffer(): String
    fun getCursor(): Int

}
