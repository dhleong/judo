package net.dhleong.judo.input

import net.dhleong.judo.IJudoCore

/**
 * @author dhleong
 */
class KeyMapHelper(
    private val judo: IJudoCore,
    private val builtinMaps: KeyMapping,
    private val userMaps: KeyMapping? = null
) {

    private val input = MutableKeys()

    fun clearInput() {
        input.clear()
    }

    /** @return True if we handled it as a mapping (or might yet) */
    suspend fun tryMappings(
        key: Key,
        allowRemap: Boolean
    ): Boolean {

        input.push(key)

        val originalMaps = builtinMaps
        val remaps = userMaps

        if (allowRemap && remaps != null) {
            remaps.match(input)?.let {
                input.clear()
                it.invoke(judo)
                return true
            }

            if (remaps.couldMatch(input)) {
                return true
            }
        }

        originalMaps.match(input)?.let {
            input.clear()
            it.invoke(judo)
            return true
        }

        if (originalMaps.couldMatch(input)) {
            return true
        }

        input.clear() // no possible matches; clear input queue
        return false
    }
}