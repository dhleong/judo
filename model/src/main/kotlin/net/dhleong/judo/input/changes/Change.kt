package net.dhleong.judo.input.changes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key

typealias UndoableAction = (InputBuffer) -> Unit

interface Undoable {
  fun apply(buffer: InputBuffer)
}

interface AmendableUndoable : Undoable

/**
 * @author dhleong
 */
class Change {
    var cursor: Int = 0
    internal val keys = ArrayList<Key>()

    val undoables = ArrayList<Undoable>()

    fun clear() {
        keys.clear()
        undoables.clear()
    }

    suspend fun apply(judo: IJudoCore) {
        judo.feedKeys(keys.asSequence(), remap = true)
        judo.exitMode() // changes should always end in normal mode
    }

    internal operator fun plusAssign(key: Key) {
        keys += key
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

    fun isEmpty() = undoables.isEmpty() && keys.isEmpty()

    fun undo(buffer: InputBuffer) {
        // apply the undoables in reverse order
        for (i in undoables.lastIndex downTo 0) {
            undoables[i].apply(buffer)
        }
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

