package net.dhleong.judo.modes

import net.dhleong.judo.CursorType
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.complete.CompletionSuggester
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.action
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.motions.wordMotion
import net.dhleong.judo.render.toFlavorable

/**
 * @author dhleong
 */
class InsertMode(
    judo: IJudoCore,
    buffer: InputBuffer,
    completions: CompletionSource,
    private val history: IInputHistory
) : BaseModeWithBuffer(judo, buffer),
    MappableMode,
    InputBufferProvider {

    override val userMappings = KeyMapping()
    override val name = "insert"

    private val mapping = KeyMapping(
        keys("<up>") to action { history.scroll(-1, clampCursor = false) },
        keys("<down>") to action { history.scroll(1, clampCursor = false) },

        keys("<alt bs>") to actionOn(wordMotion(-1, false)) { _, range ->
            buffer.deleteWithCursor(range, clampCursor = false)
        },

        // not strictly vim, but nice enough
        keys("<ctrl a>") to motionAction(toStartMotion()),
        keys("<ctrl e>") to motionAction(toEndMotion()),

        keys("<ctrl b>") to { core -> core.scrollPages(1) },
        keys("<ctrl f>") to { core -> core.scrollPages(-1) },

        keys("<ctrl r>") to { core -> core.enterMode("rsearch") }
    )
    private val input = MutableKeys()

    private val suggester = CompletionSuggester(completions)

    override fun onEnter() {
        judo.setCursorType(CursorType.PIPE)
        suggester.reset()

        if (!buffer.undoMan.isInChange) {
            // eg: enterMode() or something
            buffer.beginChangeSet()
        }
    }

    override fun onExit() {
        buffer.undoMan.finishChange()
    }

    override suspend fun feedKey(key: Key, remap: Boolean, fromMap: Boolean) {
        when {
            key == Key.ENTER -> {
                judo.submit(buffer.toString(), fromMap)
                clearBuffer()
                return
            }

            // NOTE typed events don't have a keyCode, apparently,
            //  so we use keyChar
            key.char == 'c' && key.hasCtrl() -> {
                clearBuffer()
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
        if (tryMappings(key, remap, input, mapping, userMappings)) {
            // user mappings end the current change set
            if (buffer.undoMan.isInChange) {
                // the mapping might have cancelled the change
                buffer.undoMan.finishChange()
                buffer.beginChangeSet()
            }
            return
        }

        if (key.hasCtrl()) {
            // ignore
            return
        }

        // no possible mapping; just update buffer
        buffer.type(key)
    }

    override fun renderInputBuffer() = buffer.toString().toFlavorable()
    override fun getCursor(): Int = buffer.cursor

    override fun clampCursor(buffer: InputBuffer) {
        if (buffer.cursor > buffer.size) {
            buffer.cursor = maxOf(0, buffer.size)
        }
    }

    override fun clearBuffer() {
        super.clearBuffer()
        suggester.reset()
        input.clear()
        history.resetHistoryOffset()
        buffer.undoMan.clear()
    }
}

