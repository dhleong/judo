package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.Mode
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyMapHelper
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.keys

/**
 * @author dhleong
 */
class OutputNormalMode(
    private val judo: IJudoCore
) : Mode {

    override val name: String = "output-normal"

    private val mapping = KeyMapping(
        keys("i") to { core ->
            core.exitMode()
            core.feedKeys("i")
        },
        keys("I") to { core ->
            core.exitMode()
            core.feedKeys("I")
        },
        keys("<ctrl b>") to { core -> core.scrollPages(1) },
        keys("<ctrl f>") to { core -> core.scrollPages(-1) }
    )
    private val keymaps = KeyMapHelper(judo, mapping)

    override fun onEnter() {
        judo.renderer.currentTabpage.currentWindow.isOutputFocused = true
    }

    override fun onExit() {
        judo.renderer.currentTabpage.currentWindow.isOutputFocused = false
    }

    override suspend fun feedKey(key: Key, remap: Boolean, fromMap: Boolean) {
        if (keymaps.tryMappings(key, remap)) {
            return
        }
    }

}