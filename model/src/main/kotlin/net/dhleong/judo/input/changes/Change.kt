package net.dhleong.judo.input.changes

import net.dhleong.judo.input.InputBuffer
import javax.swing.KeyStroke

typealias UndoableAction = (InputBuffer) -> Unit

interface Undoable {
  fun apply(buffer: InputBuffer)
}

interface AmendableUndoable : Undoable

/**
 * @author dhleong
 */
class Change {
    internal val myKeys = ArrayList<KeyStroke>()

    val keys: List<KeyStroke> = myKeys

    val undoables = ArrayList<Undoable>()

    fun clear() {
        myKeys.clear()
        undoables.clear()
    }

    operator fun plusAssign(key: KeyStroke) {
        myKeys += key
    }

    /**
     * Add an Undoable to be performed when undoing this Change
     */
    fun addUndoAction(undoable: UndoableAction) {
        undoables += object : Undoable {
            override fun apply(buffer: InputBuffer) = undoable(buffer)
        }
    }

    fun addUndoAction(undoable: Undoable) {
        undoables += undoable
    }

    fun isEmpty() = undoables.isEmpty() && myKeys.isEmpty()

    fun undo(buffer: InputBuffer) {
        undoables.forEach { it.apply(buffer) }
    }

    inline fun <reified T : AmendableUndoable> amendUndo(block: (T) -> Unit) {
        undoables.lastOrNull()?.let {
            if (it is T) {
                block(it)
                return
            }
        }

        val new = T::class.java.newInstance()
        undoables.add(new)
        block(new)
    }
}

