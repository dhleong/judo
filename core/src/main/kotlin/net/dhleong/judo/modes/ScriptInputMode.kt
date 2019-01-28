package net.dhleong.judo.modes

import net.dhleong.judo.CursorType
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.complete.CompletionSuggester
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.motions.wordMotion

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

    override fun feedKey(key: Key, remap: Boolean, fromMap: Boolean) {
        when {
            key == Key.ENTER -> {
                submitted = true
                judo.exitMode()
                return
            }

            key.char == 'c' && key.hasCtrl() -> {
                judo.exitMode()
                return
            }

            key.isTab() -> {
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
        // force a redraw
        judo.renderer.redraw()

        while (true) {
            val stroke = judo.readKey()
            judo.feedKey(stroke)

            // force a redraw after feedKey; this is called in a transaction,
            // so the normal semantics don't trigger another redraw.
            // FIXME: this is temporary to ensure the UI updates. A more permanent may
            // involve some sort of stacked input request system (where the default
            // input request constantly loops to send to the server). This approach
            // should make it somewhat intuitive to support editing input() and
            // CmdMode buffers via normal mode, etc in a "command line mode"

            judo.renderer.redraw()

            if (exitted && !submitted) {
                return null
            } else if (exitted) {
                return buffer.toString()
            }
        }
    }
}
