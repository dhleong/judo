package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRenderer
import net.dhleong.judo.Mode
import net.dhleong.judo.input.Key

/**
 * @author dhleong
 */
class BlockingEchoMode(
    private val judo: IJudoCore,
    private val renderer: JudoRenderer
) : Mode {
    override val name: String = "BlockingEcho"

    override suspend fun feedKey(key: Key, remap: Boolean, fromMap: Boolean) {
        judo.exitMode()
        if (key != Key.ENTER) {
            judo.feedKey(key, remap, fromMap)
        }
    }

    override fun onEnter() {
        // nop
    }

    override fun onExit() {
        renderer.clearEcho()
    }

}