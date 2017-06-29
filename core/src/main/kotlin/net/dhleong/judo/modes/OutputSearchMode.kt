package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.StateKind
import net.dhleong.judo.complete.CompletionSuggester
import net.dhleong.judo.complete.RecencyCompletionSource
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.util.hasCtrl
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

val KEY_LAST_SEARCH_STRING = StateKind<CharSequence>("net.dhleong.judo.modes.search.lastSearch")

/**
 * Mode for searching through the output
 * @author dhleong
 */
class OutputSearchMode(
    judo: IJudoCore,
    completions: RecencyCompletionSource
) : BaseModeWithBuffer(judo, InputBuffer()),
    StatusBufferProvider {

    override val name: String = "search"

    val mapping = KeyMapping(
//        keys("<up>") to { _ -> history.scroll(-1) },
//        keys("<down>") to { _ -> history.scroll(1) },

        keys("<ctrl a>") to motionAction(toStartMotion()),
        keys("<ctrl e>") to motionAction(toEndMotion())
    )
    private val input = MutableKeys()

    private val suggester = CompletionSuggester(completions)

    override fun feedKey(key: KeyStroke, remap: Boolean, fromMap: Boolean) {
        when {
            key.keyCode == KeyEvent.VK_ENTER -> {
                val searchString = buffer.toString().trim()

                clearBuffer()
                exitMode()

                judo.state[KEY_LAST_SEARCH_STRING] = searchString
                judo.searchForKeyword(searchString)
                return
            }

            key.keyChar == 'c' && key.hasCtrl() -> {
                clearBuffer()
                exitMode()
                return
            }

            // NOTE: ctrl+i == tab
            key.keyCode == KeyEvent.VK_TAB
                || key.keyChar == 'i' && key.hasCtrl() -> {
                performTabCompletionFrom(key, suggester)
                return
            }
        }

        // input changed; suggestions go away
        suggester.reset()

        // handle key mappings
        if (tryMappings(key, remap, input, mapping, null)) {
            return
        }

        if (key.hasCtrl()) {
            // ignore
            return
        }

        insertChar(key)
    }

    override fun onEnter() {
        clearBuffer()
    }

    override fun renderStatusBuffer(): String = "/$buffer"
    override fun getCursor(): Int = buffer.cursor + 1

    private fun exitMode() {
        judo.exitMode()
    }

    private fun clearBuffer() {
        buffer.clear()
        input.clear()
    }

    /**
     * Insert a key stroke at the current cursor position
     */
    private fun insertChar(key: KeyStroke) {
        val wasEmpty = buffer.isEmpty()
        buffer.type(key)
        if (buffer.isEmpty() && wasEmpty) {
            exitMode()
        }
    }

}
