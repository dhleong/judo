package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.StateKind
import net.dhleong.judo.complete.CompletionSuggester
import net.dhleong.judo.complete.RecencyCompletionSource
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.render.FlavorableStringBuilder

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

    override suspend fun feedKey(key: Key, remap: Boolean, fromMap: Boolean) {
        when {
            key == Key.ENTER -> {
                val searchString = buffer.toString().trim()

                clearBuffer()
                exitMode()

                judo.state[KEY_LAST_SEARCH_STRING] = searchString
                judo.searchForKeyword(searchString)
                return
            }

            key.char == 'c' && key.hasCtrl() -> {
                clearBuffer()
                exitMode()
                return
            }

            key.isTab() -> {
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

    override fun renderStatusBuffer() = FlavorableStringBuilder.withDefaultFlavor("/$buffer")
    override fun getCursor(): Int = buffer.cursor + 1

    private fun exitMode() {
        judo.exitMode()
    }

    override fun clearBuffer() {
        super.clearBuffer()
        input.clear()
        suggester.reset()
    }

    /**
     * Insert a key stroke at the current cursor position
     */
    private fun insertChar(key: Key) {
        val wasEmpty = buffer.isEmpty()
        buffer.type(key)
        if (buffer.isEmpty() && wasEmpty) {
            exitMode()
        }
    }

}
