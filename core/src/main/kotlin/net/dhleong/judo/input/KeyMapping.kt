package net.dhleong.judo.input

import net.dhleong.judo.IJudoCore

/**
 * @author dhleong
 */

typealias KeyAction = suspend (IJudoCore) -> Unit

inline fun action(crossinline block: suspend () -> Unit): KeyAction = {
    block()
}


data class KeyMapTarget(val action: KeyAction, val description: String)

class KeyMapping() {

    val size: Int
        get() = keysMap.size

    private val keysMap = HashMap<Keys, KeyMapTarget>()
    private val possibleMaps = HashMap<Keys, Int>()

    constructor(vararg mappings: Pair<Keys, KeyAction>) : this(listOf(*mappings))
    constructor(mappings: List<Pair<Keys, KeyAction>>) : this() {
        mappings.forEach { map(it.first, it.second) }
    }

    /**
     * @return True iff there's a mapping that starts with or is exactly
     *  `keys`
     */
    fun couldMatch(keys: Keys): Boolean =
        possibleMaps[keys]?.let { it > 0 }
            ?: false

    fun match(keys: Keys): KeyAction? = keysMap[keys]?.action

    fun map(from: Keys, to: KeyAction, description: String = "(fn)") {
        if (from.isEmpty()) throw IllegalArgumentException("Mappings must not be empty")

        keysMap[from] = KeyMapTarget(to, description.let {
            if (it.isEmpty()) "(fn)"
            else it
        })
        for (mapEnd in from.indices) {
            addPossible(from.slice(0..mapEnd), 1)
        }
    }

    fun map(from: Keys, to: Keys) = map(from, to, true)
    fun noremap(from: Keys, to: Keys) = map(from, to, false)

    fun unmap(from: Keys) {
        keysMap.remove(from)

        for (mapEnd in from.indices) {
            addPossible(from.slice(0..mapEnd), -1)
        }
    }

    override fun toString(): String = toString("")

    fun toString(prefix: String): String =
        StringBuilder(1024).apply {
            append(prefix)
            appendln("KeyMappings")
            for (i in prefix.indices) {
                append("=")
            }
            appendln("===========")
            keysMap.forEach { (k, v) ->
                k.describeTo(this)
                append('\t')
                appendln(v.description)
            }
        }.toString()

    private fun addPossible(slice: Keys, addCount: Int) {
        possibleMaps[slice]?.let {
            possibleMaps[slice] = it + addCount
            return
        }

        if (addCount > 0) {
            possibleMaps[slice] = addCount
        }
    }

    private fun map(from: Keys, to: Keys, remap: Boolean) {
        map(from, { core ->
            to.forEach {
                core.feedKey(it, remap, true)
            }
        }, StringBuilder().apply {
            to.describeTo(this)
        }.toString())
    }
}


fun keys(string: String) = Keys.parse(string)
