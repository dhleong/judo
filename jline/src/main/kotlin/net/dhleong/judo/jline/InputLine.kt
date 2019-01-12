package net.dhleong.judo.jline

import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder

/**
 * Wraps an input line and its cursor position. Mutable.
 * For use with [TerminalInputLineHelper]
 *
 * @author dhleong
 */
class InputLine(
    var line: FlavorableCharSequence = FlavorableStringBuilder.EMPTY,
    var cursorIndex: Int = 0,
    var cursorRow: Int = -1,
    var cursorCol: Int = -1
)