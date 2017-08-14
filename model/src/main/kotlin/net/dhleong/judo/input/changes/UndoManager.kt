package net.dhleong.judo.input.changes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import java.util.ArrayDeque

/**
 * @author dhleong
 */
class UndoManager {

    val current: Change
        get() = currentChange

    /** Is [current] being actively modified? */
    var isChangeActive = false
        private set

    val isInChange: Boolean
        get() = changeSetNesting > 0

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
        isChangeActive = false
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
            currentChange.cursor = buffer.cursor
        }

        ++changeSetNesting
    }

    fun initChange(initialKey: Char) {
        if (isChangeActive) {
            // don't double-init
            return
        }

        isChangeActive = true
        currentChange.let {
            // reset to just the initial key
            it.keys.clear()
            it.keys.add(Key.ofChar(initialKey))
        }
    }

    fun finishChange() {
        if (--changeSetNesting > 0) {
            // closing a changeSet, but not completely closed
            return
        } else if (changeSetNesting < 0) {
            throw IllegalStateException("Unbalanced change!")
        }

        isChangeActive = false

        if (currentChange.isEmpty()) {
            // actually, no change; don't bother
            return
        }

        lastChange = currentChange
        changeHistory.push(currentChange)
        redoStack.clear()
        currentChange = Change()
    }

    /**
     * To be called BEFORE other key processing
     */
    fun onKeyStroke(key: Key) {
        // NOTE: obviously it'd be nice if this were actually called *after*
        //  normal key processing, so we don't have to manually provide the
        //  initial key to [initChange], but due to the blocking nature of
        //  [readKey()], and the fact that [readKey()] is the only place to
        //  handle this and be able to include ALL keystrokes, we have
        //  to settle for after.
        if (isChangeActive) {
            // only add the keystroke to the change if we've already
            // initialized the change
            currentChange += key
        }
    }

    fun undo(input: InputBuffer) {
        withoutTrackingChanges(input) {
            if (changeHistory.isEmpty()) {
                // nothing to undo
            } else {
                val undone = changeHistory.pop()
                redoStack.push(undone)

                undone.undo(input)
            }
        }
    }

    fun redo(judo: IJudoCore, input: InputBuffer) {
        withoutTrackingChanges(input) {
            if (redoStack.isEmpty()) {
                // nothing to redo
            } else {
                val redone = redoStack.pop()
                changeHistory.push(redone)

                input.cursor = redone.cursor
                redone.apply(judo)
            }
        }
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

