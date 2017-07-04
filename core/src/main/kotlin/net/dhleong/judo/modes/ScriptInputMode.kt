package net.dhleong.judo.modes

import net.dhleong.judo.CursorType
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.complete.CompletionSuggester
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.motions.wordMotion
import net.dhleong.judo.util.hasCtrl
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
class ScriptInputMode(
    judo: IJudoCore,
    completions: CompletionSource,
    val prompt: String = ""
) : BaseModeWithBuffer(judo, InputBuffer()),
    InputBufferProvider {

    override val name = "input"

    private val mapping = KeyMapping(
        keys("<alt bs>") to actionOn(wordMotion(-1, false)) { _, range ->
            buffer.deleteWithCursor(range, clampCursor = false)
        },

        keys("<ctrl a>") to motionAction(toStartMotion()),
        keys("<ctrl e>") to motionAction(toEndMotion())
    )
    private val input = MutableKeys()
    private val suggester = CompletionSuggester(completions)

    private var exitted = false
    private var submitted = false

    override fun onEnter() {
        judo.setCursorType(CursorType.PIPE)
        suggester.reset()
    }

    override fun onExit() {
        exitted = true
    }

    override fun feedKey(key: KeyStroke, remap: Boolean, fromMap: Boolean) {
        when {
            key.keyCode == KeyEvent.VK_ENTER -> {
                submitted = true
                judo.exitMode()
                return
            }

            key.keyChar == 'c' && key.hasCtrl() -> {
                judo.exitMode()
                return
            }

            key.keyCode == KeyEvent.VK_TAB
                || key.keyChar == 'i' && key.hasCtrl() -> {
                performTabCompletionFrom(key, suggester)
                return
            }
        }

        // handle key mappings
        if (tryMappings(key, remap, input, mapping, null)) {
            return
        }

        if (key.hasCtrl()) {
            // ignore
            return
        }

        // no possible mapping; just update buffer
        buffer.type(key)
    }

    override fun renderInputBuffer(): String =
        "$prompt$buffer"

    override fun getCursor(): Int =
        prompt.length + buffer.cursor

    fun awaitResult(): String? {
        while (true) {
            val stroke = judo.readKey()
            judo.feedKey(stroke)

            if (exitted && !submitted) {
                return null
            } else if (exitted) {
                return buffer.toString()
            }
        }
    }
}
