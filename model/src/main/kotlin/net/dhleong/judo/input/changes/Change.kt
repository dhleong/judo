package net.dhleong.judo.input.changes

import net.dhleong.judo.input.InputBuffer
import javax.swing.KeyStroke

typealias Undoable = (InputBuffer) -> Unit

/**
 * @author dhleong
 */
class Change {
    internal val keys = ArrayList<KeyStroke>()

    private val undoables = ArrayList<Undoable>()

    fun clear() {
        keys.clear()
    }

    operator fun plusAssign(key: KeyStroke) {
        keys += key
    }

    /**
     * Add an Undoable to be performed when undoing this Change
     */
    fun addUndoAction(undoable: Undoable) {
        undoables += undoable
    }

    fun undo(buffer: InputBuffer) {
        undoables.forEach { it(buffer) }
    }
}

