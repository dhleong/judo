package net.dhleong.judo.input

import net.dhleong.judo.IJudoCore
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

typealias KeyAction = (IJudoCore) -> Unit

class KeyMapping() {

    val size: Int
        get() = keysMap.size

    private val keysMap = HashMap<Keys, KeyAction>()
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

    fun match(keys: Keys): KeyAction? = keysMap[keys]

    fun map(from: Keys, to: KeyAction) {
        if (from.isEmpty()) throw IllegalArgumentException("Mappings must not be empty")

        keysMap[from] = to
        (0 until from.size).forEach { mapEnd ->
            addPossible(from.slice(0..mapEnd), 1)
        }
    }

    fun map(from: Keys, to: Keys) = map(from, to, true)
    fun noremap(from: Keys, to: Keys) = map(from, to, false)

    fun unmap(from: Keys) {
        keysMap.remove(from)

        (0 until from.size).forEach { mapEnd ->
            addPossible(from.slice(0..mapEnd), -1)
        }
    }

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
        })
    }
}

fun key(string: String): KeyStroke {
    val stroke: String
    when (string) {
        // special cases
        " ", "20", "space" -> return KeyStroke.getKeyStroke(' ')
        "bs" -> return KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
        "alt bs" -> return KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.ALT_DOWN_MASK)
        "cr" -> return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        "esc" -> return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
        "up" -> return KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)
        "down" -> return KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)

        else -> {
            if ("typed" !in string) {
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
        }
    }

    return KeyStroke.getKeyStroke(stroke)
        ?: throw IllegalArgumentException("Unable to parse `$stroke` into a KeyStroke")
}

fun keys(string: String) = Keys.parse(string)
