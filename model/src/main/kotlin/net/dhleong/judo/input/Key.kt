package net.dhleong.judo.input

import java.util.EnumSet

/**
 * @author dhleong
 */
enum class Modifier {
    ALT,
    CTRL,
    SHIFT;

    companion object {
        internal val NONE = EnumSet.noneOf(Modifier::class.java)
    }
}

class Key private constructor(
    val char: Char,
    val keyCode: Int = char.toInt(),
    val modifiers: EnumSet<Modifier> = Modifier.NONE
) {
    companion object {
        val CODE_BACKSPACE = 8
        val CODE_ENTER = 13
        val CODE_ESCAPE = 27
        val CODE_TAB = '\t'.toInt()

        val CODE_LEFT = 128
        val CODE_UP = 129
        val CODE_RIGHT = 130
        val CODE_DOWN = 131

        val CODE_PAGEUP = 132
        val CODE_PAGEDOWN = 133

        val BACKSPACE: Key by lazy { ofChar(CODE_BACKSPACE.toChar()) }
        val ESCAPE: Key by lazy { ofChar(CODE_ESCAPE.toChar()) }
        val ENTER: Key by lazy { ofChar(CODE_ENTER.toChar()) }

        private val cache = mutableMapOf<String, Key>()
        private val charCache = arrayOfNulls<Key>(255)

        fun parse(string: String): Key {
            if (string.length == 1) return ofChar(string[0])
            if (string.isEmpty()) throw IllegalArgumentException("key cannot be empty")

            cache[string]?.let { return it }

            when (string) {
                "bs" -> BACKSPACE
                "esc" -> ESCAPE
                "cr" -> ENTER
            }

            doParse(string).let { key ->
                cache[string] = key
                return key
            }
        }

        fun ofChar(char: Char): Key {
            val index = char.toInt()
            if (index >= charCache.size) throw IllegalArgumentException("Cannot parse `$char`")

            charCache[char.toInt()]?.let { return it }

            doParse(char).let { key ->
                charCache[char.toInt()] = key
                return key
            }
        }

        private fun doParse(string: String): Key {
            val len = string.length
            val modifiers = mutableSetOf<Modifier>()
            var start = 0
            var end = 0

            while (end < len) {
                val curr = string[end]
                if (curr == ' ' || curr == '-') {
                    val readChar = readPart(string, start, end, modifiers)
                    if (readChar != 0.toChar()) {
                        return Key(readChar, modifiers)
                    }

                    start = end + 1
                    end = start
                } else {
                    ++end
                }
            }

            if (start < end) {
                val readChar = readPart(string, start, end, modifiers)
                if (readChar != 0.toChar()) {
                    return Key(readChar, modifiers)
                }
            }

            throw IllegalArgumentException("Unable to parse `$string` into a Key")
        }

        private fun readPart(
            string: String,
            start: Int, end: Int,
            modifiers: MutableSet<Modifier>
        ): Char {
            val len = end - start
            if (len == 1 && end == string.length) {
                return Character.toLowerCase(string[start])
            }

            val key = string.substring(start, end).toLowerCase()
            if (end < string.length) {
                when (key) {
                    "a", "alt" -> Modifier.ALT
                    "c", "ctrl" -> Modifier.CTRL
                    "s", "shift" -> Modifier.SHIFT
                    else -> null
                }?.let {
                    modifiers.add(it)
                    return 0.toChar()
                }
            }

            // regular key
            return when (key) {
                " ", "20", "space" -> ' '.toInt()
                "bs", "backspace" -> CODE_BACKSPACE
                "cr" -> CODE_ENTER
                "esc" -> CODE_ESCAPE
                "tab" -> CODE_TAB

                "left" -> CODE_LEFT
                "up" -> CODE_UP
                "right" -> CODE_RIGHT
                "down" -> CODE_DOWN

                "pageup" -> CODE_PAGEUP
                "pagedown" -> CODE_PAGEDOWN

                else -> throw IllegalArgumentException(
                    "Unknown key `$key`"
                )
            }.toChar()
        }

        private fun doParse(char: Char): Key {
            val asInt = char.toInt()
            if (asInt >= 'A'.toInt() && asInt <= 'Z'.toInt()) {
                val lower = Character.toLowerCase(char)
                return Key(lower, setOf(Modifier.SHIFT))
            }

            return Key(char)
        }
    }

    private constructor(char: Char, modifiers: Set<Modifier>) : this(
        char,
        modifiers =
            if (modifiers.isNotEmpty()) EnumSet.copyOf(modifiers)
            else EnumSet.noneOf(Modifier::class.java)
    )

    override fun equals(other: Any?): Boolean {
        if (other !is Key) return false
        return other.keyCode == keyCode
            && other.modifiers == modifiers
    }

    override fun hashCode(): Int {
        var result = keyCode
        result = 31 * result + modifiers.hashCode()
        return result
    }

    fun describe(): String =
        StringBuilder(32).apply {
            describeTo(this)
        }.toString()

    fun describeTo(out: Appendable) {
        val specialKey = when (keyCode) {
            ' '.toInt() -> "space"
            Key.CODE_BACKSPACE -> "bs"
            Key.CODE_ENTER -> "cr"
            Key.CODE_ESCAPE -> "esc"
            Key.CODE_TAB -> "tab"

            Key.CODE_LEFT -> "left"
            Key.CODE_UP -> "up"
            Key.CODE_RIGHT -> "right"
            Key.CODE_DOWN -> "down"

            Key.CODE_PAGEUP -> "PageUp"
            Key.CODE_PAGEDOWN -> "PageDown"

            else -> null
        }

        val inBrackets = specialKey != null || modifiers.isEmpty()
        if (inBrackets) out.append('<')

        if (hasAlt()) out.append("alt ")
        if (hasCtrl()) out.append("ctrl ")
        if (hasShift() && specialKey != null) out.append("shift ")

        if (specialKey != null) {
            out.append(specialKey)
        } else {
            out.append(char)
        }

        if (inBrackets) out.append('>')
    }

    fun hasAlt() = Modifier.ALT in modifiers
    fun hasCtrl() = Modifier.CTRL in modifiers
    fun hasShift() = Modifier.SHIFT in modifiers
}