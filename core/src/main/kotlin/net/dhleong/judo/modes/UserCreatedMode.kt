package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
class UserCreatedMode(
    val judo: IJudoCore,
    override val name: String
) : MappableMode {

    override val userMappings = KeyMapping()
    private val input = MutableKeys()

    override fun onEnter() {
        input.clear()
    }

    override fun feedKey(key: KeyStroke, remap: Boolean) {
        input.push(key)

        userMappings.match(input)?.let {
            input.clear()
            it.invoke(judo)
            return
        }

        if (userMappings.couldMatch(input)) {
            return
        }

        input.clear() // no possible matches; clear input queue
    }

    override fun toString(): String {
        return "[UserMode($name)]"
    }
}
