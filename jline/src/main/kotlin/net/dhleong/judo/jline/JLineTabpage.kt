package net.dhleong.judo.jline

import net.dhleong.judo.StateMap
import net.dhleong.judo.render.*
import org.jline.utils.AttributedCharSequence
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder

/**
 * @author dhleong
 */
abstract class JLineTabpage(
    ids: IdManager,
    settings: StateMap,
    initialWindow: JLineWindow
) : IJudoTabpage2 {

    fun render(display: JLineDisplay) {
        TODO()
    }
}