package net.dhleong.judo.input

import net.dhleong.judo.IJudoCore
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

typealias KeyAction = (IJudoCore) -> Unit

class KeyMapping() {

    private val keysMap = HashMap<Keys, KeyAction>()
    private val possibleMaps = HashSet<Keys>()

    constructor(vararg mappings: Pair<Keys, KeyAction>) : this() {
        mappings.forEach { map(it.first, it.second) }
    }

    /**
     * @return True iff there's a mapping that starts with or is exactly
     *  `keys`
     */
    fun couldMatch(keys: Keys): Boolean = possibleMaps.contains(keys)

    fun match(keys: Keys): KeyAction? = keysMap[keys]

    fun map(from: Keys, to: KeyAction) {
        if (from.isEmpty()) throw IllegalArgumentException("Mappings must not be empty")

        keysMap[from] = to
        (0 until from.size).forEach { mapEnd ->
            possibleMaps.add(from.slice(0..mapEnd))
        }
    }

    fun map(from: Keys, to: Keys) = map(from, to, true)
    fun noremap(from: Keys, to: Keys) = map(from, to, false)

    private fun map(from: Keys, to: Keys, remap: Boolean) {
        map(from, { core ->
            to.forEach {
                core.feedKey(it, remap)
            }
        })
    }
}

fun key(string: String): KeyStroke {
    val stroke: String
    if (string == " " || string == "20" || string == "space") {
        return KeyStroke.getKeyStroke(' ')
    } else if (string == "cr") {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
    } else if ("typed" !in string) {
        val lastSpace = string.lastIndexOf(' ')
        if (lastSpace == -1) {
            stroke = "typed $string"
        } else {
            val before = string.slice(0..lastSpace)
            val after = string.substring(lastSpace + 1)
            stroke = "$before typed $after"
        }
    } else {
        stroke = string
    }

    return KeyStroke.getKeyStroke(stroke)
        ?: throw IllegalArgumentException("Unable to parse `$stroke` into a KeyStroke")
}

fun keys(vararg strings: String): Keys =
    Keys.of(strings.map { key(it) })
