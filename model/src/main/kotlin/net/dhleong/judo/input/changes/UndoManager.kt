package net.dhleong.judo.input.changes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.input.InputBuffer
import java.util.ArrayDeque

/**
 * @author dhleong
 */
class UndoManager {

    val current: Change
        get() = currentChange

    /** The current size of the undo history */
    val size: Int
        get() = changeHistory.size

    private val changeHistory = ArrayDeque<Change>()
    private val redoStack = ArrayDeque<Change>()

    private var currentChange = Change()
    var lastChange: Change? = null
        private set

    /**
     * Each time we beginChangeSet, we're creating
     * a "nested" change, which means everything until
     * the that nesting returns to zero is part of the
     * same change.
     */
    private var changeSetNesting = 0

    fun cancelChange() {
        currentChange.clear()
    }

    fun clear() {
        changeHistory.clear()
        redoStack.clear()
    }

    inline fun inChangeSet(buffer: InputBuffer, block: () -> Unit) {
        beginChangeSet(buffer)
        try {
            block()
        } finally {
            finishChange()
        }
    }

    fun beginChangeSet(buffer: InputBuffer) {
        if (changeSetNesting == 0) {
            // save initial buffer.cursor
            currentChange.cursor = buffer.cursor
        }
        ++changeSetNesting
    }

    fun finishChange() {
        if (--changeSetNesting > 0) {
            // closing a changeSet, but not completely closed
            return
        } else if (changeSetNesting < 0) {
            throw IllegalStateException("Unbalanced change!")
        }

        if (currentChange.isEmpty()) {
            // actually, no change; don't bother
            return
        }

        lastChange = currentChange
        changeHistory.push(currentChange)
        redoStack.clear()
        currentChange = Change()
    }

    fun undo(input: InputBuffer) {
        if (changeHistory.isEmpty()) {
            // nothing to undo
            return
        }

        val undone = changeHistory.pop()
        redoStack.push(undone)

        undone.undo(input)
    }

    fun redo(judo: IJudoCore, input: InputBuffer) {
        if (redoStack.isEmpty()) {
            // nothing to redo
            return
        }

        val redone = redoStack.pop()
        changeHistory.push(redone)

        input.cursor = redone.cursor
        redone.apply(judo)
    }

    /**
     * Perform block() without tracking changes (IE the
     * undo history will not be affected)
     */
    fun withoutTrackingChanges(buffer: InputBuffer, block: () -> Unit) {
        inChangeSet(buffer) {
            try {
                block()
            } finally {
                cancelChange()
            }
        }
    }
}

