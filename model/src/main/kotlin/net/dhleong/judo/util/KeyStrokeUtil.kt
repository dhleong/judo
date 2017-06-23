package net.dhleong.judo.util

import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

fun KeyStroke.describe(): String =
    StringBuilder(32).apply {
        describeTo(this)
    }.toString()

fun KeyStroke.describeTo(out: Appendable) {
    val specialKey = when (keyCode) {
        KeyEvent.VK_SPACE -> "space" // normally keyCode is probably 0 here, though
        KeyEvent.VK_BACK_SPACE -> "bs"
        KeyEvent.VK_ENTER -> "cr"
        KeyEvent.VK_ESCAPE -> "esc"
        KeyEvent.VK_UP -> "up"
        KeyEvent.VK_DOWN -> "down"

        else ->
            if (keyChar == ' ') "space"
            else null
    }

    val inBrackets = specialKey != null || modifiers != 0
    if (inBrackets) out.append('<')

    if (hasAlt()) out.append("alt ")
    if (hasCtrl()) out.append("ctrl ")
    if (hasShift() && specialKey != null) out.append("shift ")

    if (specialKey != null) {
        out.append(specialKey)
    } else {
        out.append(keyChar)
    }

    if (inBrackets) out.append('>')
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

fun KeyStroke.hasAlt(): Boolean =
    hasModifiers(KeyEvent.ALT_DOWN_MASK)

fun KeyStroke.hasCtrl(): Boolean =
    hasModifiers(KeyEvent.CTRL_DOWN_MASK)

fun KeyStroke.hasShift(): Boolean =
    hasModifiers(KeyEvent.SHIFT_DOWN_MASK)

fun KeyStroke.hasModifiers(mask: Int): Boolean =
    (modifiers and mask) != 0

