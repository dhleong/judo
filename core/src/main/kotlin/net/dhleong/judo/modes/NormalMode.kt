package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.OperatorFunc
import net.dhleong.judo.input.CountReadingBuffer
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapHelper
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.action
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.ALL_MOTIONS
import net.dhleong.judo.motions.Motion
import net.dhleong.judo.motions.charMotion
import net.dhleong.judo.motions.normalizeForMotion
import net.dhleong.judo.motions.repeat
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.motions.xCharMotion
import net.dhleong.judo.render.toFlavorable
import net.dhleong.judo.util.VisibleForTesting

/**
 * @author dhleong
 */

class NormalMode(
    judo: IJudoCore,
    buffer: InputBuffer,
    @VisibleForTesting
    internal val history: IInputHistory,
    private val opMode: OperatorPendingMode
) : BaseModeWithBuffer(judo, buffer),
    MappableMode,
    InputBufferProvider {

    override val userMappings = KeyMapping()
    override val name = "normal"

    private val mapping = KeyMapping(listOf(
        keys(":") to { core -> core.enterMode("cmd") },
        keys("/") to { core -> core.enterMode("search") },

        keys("a") to { core ->
            applyMotion(charMotion(1), clampCursor = false)
            buffer.undoMan.initChange('a')
            buffer.beginChangeSet()
            core.enterMode("insert")
        },
        keys("A") to { core ->
            // it's just $a
            core.feedKeys("\$a")
        },

        keys("c") to { core ->
            withOperator('c') { range ->
                buffer.beginChangeSet()
                if (!buffer.deleteWithCursor(range, clampCursor = false)) {
                    buffer.undoMan.cancelChange()
                    // TODO bell?
                }
                core.enterMode("insert")
            }
        },
        keys("C") to { core ->
            buffer.undoMan.initChange('C')
            buffer.beginChangeSet()
            buffer.delete(rangeOf(toEndMotion()))
            core.enterMode("insert")
        },

        keys("d") to action {
            // TODO bell on error?
            withOperator('d') { range -> buffer.deleteWithCursor(range) }
        },
        keys("D") to action {
            buffer.undoMan.initChange('D')
            buffer.deleteWithCursor(rangeOf(toEndMotion()))
        },

        keys("gu") to action {
            withOperator('u') { range ->
                buffer.replaceWithCursor(range) { old ->
                    old.toString().toLowerCase()
                }
            }
        },
        keys("gU") to action {
            withOperator('U') { range ->
                buffer.replaceWithCursor(range) { old ->
                    old.toString().toUpperCase()
                }
            }
        },

        keys("G") to { core -> core.scrollToBottom() },

        keys("i") to { core ->
            buffer.undoMan.initChange('i')
            buffer.beginChangeSet()
            core.enterMode("insert")
        },
        keys("I") to { core ->
            applyMotion(toStartMotion())
            buffer.undoMan.initChange('I')
            buffer.beginChangeSet()
            core.enterMode("insert")
        },

        // browse history
        keys("j") to withCount { count -> history.scroll(count) },
        keys("k") to withCount { count -> history.scroll(-count) },

        keys("n") to withCount { count -> continueSearch(count) },
        keys("N") to withCount { count -> continueSearch(-count) },

        keys("p") to pasteWithOffset('p', 1),
        keys("P") to pasteWithOffset('P', 0),

        keys("r") to actionOnCount(::xCharMotion, 1) { _, range ->
            buffer.undoMan.initChange('r')
            val replacement = judo.readKey()
            if (replacement.hasCtrl()
                || replacement == Key.ESCAPE) {
                // TODO beep?
            } else if (!buffer.isEmpty()) {
                val char = replacement.char.toString()
                buffer.replace(range, char.repeat(range.last - range.first + 1))
            }
        },

        keys("u") to countRepeatable { buffer.undoMan.undo(buffer) },
        keys("<ctrl r>") to countRepeatable {
            buffer.undoMan.redo(judo, buffer)
        },

        keys("x") to actionOnCount(::xCharMotion, 1) { _, range ->
            buffer.undoMan.initChange('x')
            buffer.delete(range)
        },
        keys("X") to actionOnCount(::xCharMotion, -1) { _, range ->
            buffer.undoMan.initChange('X')
            buffer.delete(range)
            buffer.cursor = range.last
        },

        keys("y") to action {
            // TODO bell on error?
            withOperator('y') { range ->
                judo.registers.current.value =
                    buffer.get(range).toString()
            }
        },
        keys("Y") to action {
            judo.registers.current.value = buffer.toString()
        },

        keys(".") to countRepeatable {
            // clear out anything from the `.`; there shouldn't be
            // anything yet, but this shouldn't hurt
            buffer.undoMan.cancelChange()

            // create a new change set and apply the keystrokes from
            // the previous change. This should let us handle the case
            // where we can't cleanly apply the last change in our
            // current location (since the new change will be similar
            // but maybe not identical; this seems to be what vim does)
            buffer.inChangeSet {
                // perform the previous change without tracking undo
                buffer.undoMan.lastChange?.apply(judo)
            }
        },

        keys("~") to actionOnCount(::charMotion, 1) { _, range ->
            buffer.undoMan.initChange('~')
            if (range.first <= buffer.lastIndex) { // TODO can we generalize this?
                buffer.switchCaseWithCursor(range)
                buffer.cursor = minOf(buffer.lastIndex, range.last)
            }
        },
        keys("g~") to action {
            withOperator('~') { range ->
                buffer.switchCaseWithCursor(range)
            }
        },

        keys("\"") to { core ->
            buffer.undoMan.initChange('"')
            val register = judo.readKey()
            if (register.hasCtrl()
                || register == Key.ESCAPE) {
                // TODO beep?
            } else {
                core.registers.current = core.registers[register.char]
            }
        },

        keys("<up>") to withCount { count -> history.scroll(-count) },
        keys("<down>") to withCount { count -> history.scroll(count) },

        keys("<ctrl-b>") to withCount { count -> judo.scrollPages(count) },
        keys("<ctrl-f>") to withCount { count -> judo.scrollPages(-count) },
        keys("<ctrl-y>") to withCount { count -> judo.scrollLines(count) },
        keys("<ctrl-e>") to withCount { count -> judo.scrollLines(-count) },
        keys("<ctrl-u>") to withCount { count -> judo.scrollBySetting(count) },
        keys("<ctrl-d>") to withCount { count -> judo.scrollBySetting(-count) },

        keys("<ctrl-c>") to action { clearBuffer() },
        keys("<ctrl-s>") to { core -> core.enterMode("rsearch") },

        keys("<ctrl-w>N") to { core -> core.enterMode("output-normal") },
        keys("<ctrl-BACKSLASH><ctrl-n>") to { core -> core.enterMode("output-normal") },

        // window commands

        keys("<ctrl-w>k") to withCount { count -> judo.renderer.focusUp(count) },
        keys("<ctrl-w><ctrl-k>") to withCount { count -> judo.renderer.focusUp(count) },
        keys("<ctrl-w><up>") to withCount { count -> judo.renderer.focusUp(count) },

        keys("<ctrl-w>j") to withCount { count -> judo.renderer.focusDown(count) },
        keys("<ctrl-w><ctrl-j>") to withCount { count -> judo.renderer.focusDown(count) },
        keys("<ctrl-w><down>") to withCount { count -> judo.renderer.focusDown(count) },

        keys("<ctrl-w>h") to withCount { count -> judo.renderer.focusLeft(count) },
        keys("<ctrl-w><ctrl-h>") to withCount { count -> judo.renderer.focusLeft(count) },
        keys("<ctrl-w><left>") to withCount { count -> judo.renderer.focusLeft(count) },

        keys("<ctrl-w>l") to withCount { count -> judo.renderer.focusRight(count) },
        keys("<ctrl-w><ctrl-l>") to withCount { count -> judo.renderer.focusRight(count) },
        keys("<ctrl-w><right>") to withCount { count -> judo.renderer.focusRight(count) }

    ) + ALL_MOTIONS.filter { (_, motion) ->
        // text object motions can't be used as an action
        !motion.isTextObject
    }.map { (keys, motion) ->
        keys to motionActionWithCount(motion)
    })

    private val keymaps = KeyMapHelper(judo, mapping, userMappings)

    private fun continueSearch(direction: Int) {
        judo.state[KEY_LAST_SEARCH_STRING]?.let {
            judo.searchForKeyword(it, direction)
            return
        }

        // TODO bell?
    }

    private fun motionActionWithCount(motion: Motion): KeyAction =
        { applyMotion(repeat(motion, count.toRepeatCount())) }

    private fun withOperator(action: OperatorFunc) {
        // save now before we clear when leaving normal mode
        val repeats = count.toRepeatCount()

        judo.state[KEY_OPFUNC] = { originalRange ->

            action(originalRange)

            if (repeats > 1) {
                judo.state[KEY_LAST_OP]?.let { lastOp ->
                    // TODO repeat
                    val range = repeat(lastOp.toRepeatable(), repeats - 1)
                        .calculate(judo, buffer)
                        .normalizeForMotion(lastOp)

                    action(range)
                }
            }
        }

        fromOpMode = true
        judo.enterMode("op")
    }

    private fun withOperator(fullLineMotionKey: Char, action: OperatorFunc) {
        if (fullLineMotionKey != 'y') {
            buffer.undoMan.initChange(fullLineMotionKey)
        }

        opMode.fullLineMotionKey = fullLineMotionKey
        withOperator(action)
    }


    private val count = CountReadingBuffer()

    private var fromOpMode = false

    override fun onEnter() {
        keymaps.clearInput()

        if (!fromOpMode) {
            buffer.cursor = maxOf(0, buffer.cursor - 1)
        }

        fromOpMode = false
    }

    override suspend fun feedKey(key: Key, remap: Boolean, fromMap: Boolean) {
        if (key == Key.ENTER) {
            judo.submit(buffer.toString(), fromMap)
            clearBuffer()
            return
        }

        // read in counts
        if (count.tryPush(key)) {
            // still reading...
            buffer.undoMan.initChange(key.char)
            return
        }

        // handle key mappings
        if (keymaps.tryMappings(key, remap)) {
            // executed; clear the count
            count.clear()
            return
        }
    }

    override fun renderInputBuffer() = buffer.toString().toFlavorable()
    override fun getCursor(): Int = buffer.cursor

    override fun clearBuffer() {
        super.clearBuffer()
        count.clear()
        keymaps.clearInput()
        buffer.undoMan.clear()
        history.resetHistoryOffset()
    }

    private fun actionOnCount(motionFactory: (Int) -> Motion, step: Int, action: KeyActionOnRange): KeyAction {
        return { core ->
            val motionWithCount = motionFactory(step * count.toRepeatCount())
            actionOn(motionWithCount, action).invoke(core)
        }
    }

    /**
     * Given a singular action, return an action which can repeat
     * that action multiple times based on typed count
     */
    private fun countRepeatable(action: KeyAction): KeyAction =
        { judo ->
            val repeats = count.toRepeatCount()
            for (i in 1..repeats) {
                action(judo)
            }
        }

    private fun withCount(action: (Int) -> Unit): KeyAction =
        { action.invoke(count.toRepeatCount()) }

    private fun pasteWithOffset(keyChar: Char, offset: Int): KeyAction = withCount { count ->
        buffer.undoMan.initChange(keyChar)

        val value = judo.registers.current.value.repeat(count)
        judo.registers.resetCurrent()

        buffer.insert(minOf(buffer.size, buffer.cursor + offset), value)
        buffer.cursor += value.length
    }
}


