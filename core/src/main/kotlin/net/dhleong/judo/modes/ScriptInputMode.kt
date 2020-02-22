package net.dhleong.judo.modes

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import net.dhleong.judo.CursorType
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.complete.CompletionSuggester
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyMapHelper
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.motions.wordMotion
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.toFlavorable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @author dhleong
 */
class ScriptInputMode(
    judo: IJudoCore,
    completions: CompletionSource,
    override val buffer: InputBuffer,
    private val history: IInputHistory,
    val prompt: String = ""
) : BaseModeWithBuffer(judo, buffer),
    InputBufferProvider {

    override val name = "input"

    private val mapping = KeyMapping(
        keys("<alt bs>") to actionOn(wordMotion(-1, false)) { _, range ->
            buffer.deleteWithCursor(range, clampCursor = false)
        },

        keys("<ctrl a>") to motionAction(toStartMotion()),
        keys("<ctrl e>") to motionAction(toEndMotion())
    )

    private val keymaps = KeyMapHelper(judo, mapping)

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

    override suspend fun feedKey(key: Key, remap: Boolean, fromMap: Boolean) {
        when {
            key == Key.ENTER -> {
                submitted = true
                history.push(buffer.toString())
                judo.exitMode()
                return
            }

            key.char == 'c' && key.hasCtrl() -> {
                judo.exitMode()
                return
            }

            key.char == 'f' && key.hasCtrl() -> {
                val result = judo.readCommandLineInput('@', history, buffer.toString())
                if (result != null) {
                    buffer.set(result)
                    submitted = true
                    judo.exitMode()
                }
                return
            }

            key.isTab() -> {
                performTabCompletionFrom(key, suggester)
                return
            }
        }

        // handle key mappings
        if (keymaps.tryMappings(key, remap)) {
            return
        }

        if (key.hasCtrl()) {
            // ignore
            return
        }

        // no possible mapping; just update buffer
        buffer.type(key)
    }

    override fun renderInputBuffer(): FlavorableCharSequence =
        "$prompt$buffer".toFlavorable()

    override fun getCursor(): Int =
        prompt.length + buffer.cursor

    override fun clearBuffer() {
        super.clearBuffer()
        suggester.reset()
    }

    suspend fun awaitResult(): String? = suspendCoroutine { cont ->
        GlobalScope.launch {
            while (!exitted) {
                val stroke = try {
                    judo.readKey()
                } catch (e: ClosedReceiveChannelException) {
                    // NOTE: should be unit tests only
                    // FIXME: can we handle this better? cancel the scope maybe?
                    cont.resume(null)
                    return@launch
                }
                judo.feedKey(stroke)
            }

            cont.resume(
                if (!submitted) null
                else buffer.toString()
            )
        }
    }
}
