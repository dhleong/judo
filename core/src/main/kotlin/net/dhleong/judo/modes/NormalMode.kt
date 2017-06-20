package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.OperatorFunc
import net.dhleong.judo.input.CountReadingBuffer
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.ALL_MOTIONS
import net.dhleong.judo.motions.Motion
import net.dhleong.judo.motions.charMotion
import net.dhleong.judo.motions.normalizeForMotion
import net.dhleong.judo.motions.repeat
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.motions.xCharMotion
import net.dhleong.judo.util.InputHistory
import net.dhleong.judo.util.hasCtrl
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

class NormalMode(
        judo: IJudoCore,
        buffer: InputBuffer,
        val history: InputHistory,
        val opMode: OperatorPendingMode
) : BaseModeWithBuffer(judo, buffer),
    MappableMode,
    InputBufferProvider {

    override val userMappings = KeyMapping()
    override val name = "normal"

    private val mapping = KeyMapping(listOf(
        keys(":") to { core -> core.enterMode("cmd") },
        keys("/") to { core -> core.enterMode("search") },

        keys("a") to { core ->
            applyMotion(charMotion(1))
            core.enterMode("insert")
        },
        keys("A") to { core ->
            applyMotion(toEndMotion())
            core.enterMode("insert")
        },

        keys("c") to { core ->
            withOperator('c') { range ->
                if (!buffer.deleteWithCursor(range, clampCursor = false)) {
                    // TODO bell?
                }
                core.enterMode("insert")
            }
        },
        keys("C") to { core ->
            buffer.delete(rangeOf(toEndMotion()))
            core.enterMode("insert")
        },

        keys("d") to { _ ->
            // TODO bell on error?
            withOperator('d') { range -> buffer.deleteWithCursor(range) }
        },
        keys("D") to { _ ->
            buffer.deleteWithCursor(rangeOf(toEndMotion()))
        },

        keys("gu") to { _ ->
            withOperator('u') { range ->
                buffer.replaceWithCursor(range) { old ->
                    old.toString().toLowerCase()
                }
            }
        },
        keys("gU") to { _ ->
            withOperator('U') { range ->
                buffer.replaceWithCursor(range) { old ->
                    old.toString().toUpperCase()
                }
            }
        },

        keys("G") to { core -> core.scrollToBottom() },

        keys("i") to { core -> core.enterMode("insert") },
        keys("I") to { core ->
            applyMotion(toStartMotion())
            core.enterMode("insert")
        },

        // browse history
        keys("j") to withCount { count -> history.scroll(count) },
        keys("k") to withCount { count -> history.scroll(-count) },

        keys("n") to withCount { count -> continueSearch(count) },
        keys("N") to withCount { count -> continueSearch(-count) },

        keys("r") to actionOnCount(::xCharMotion, 1) { _, range ->
            val replacement = judo.readKey()
            if (replacement.hasCtrl()
                    || replacement.keyCode == KeyEvent.VK_ESCAPE) {
                // TODO beep?
            } else {
                val char = replacement.keyChar.toString()
                buffer.replace(range, char.repeat(range.endInclusive - range.start + 1))
            }
        },

        keys("x") to actionOnCount(::xCharMotion, 1) { _, range ->
            buffer.delete(range)
        },
        keys("X") to actionOnCount(::xCharMotion, -1) { _, range ->
            buffer.delete(range)
            buffer.cursor = range.endInclusive
        },

        keys("~") to actionOnCount(::charMotion, 1) { _, range ->
            if (range.start <= buffer.lastIndex) { // TODO can we generalize this?
                buffer.switchCaseWithCursor(range)
                buffer.cursor = minOf(buffer.lastIndex, range.endInclusive)
            }
        },
        keys("g~") to { _ ->
            withOperator('~') { range ->
                buffer.switchCaseWithCursor(range)
            }
        },

        keys("<up>") to withCount { count -> history.scroll(-count) },
        keys("<down>") to withCount { count -> history.scroll(count) },

        keys("<ctrl b>") to withCount { count -> judo.scrollPages(count) },
        keys("<ctrl f>") to withCount { count -> judo.scrollPages(-count) },
        keys("<ctrl c>") to { _ -> clearBuffer() },
        keys("<ctrl r>") to { core -> core.enterMode("rsearch") }

    ) + ALL_MOTIONS.filter { (_, motion) ->
        // text object motions can't be used as an action
        !motion.isTextObject
    }.map { (keys, motion) ->
        keys to motionActionWithCount(motion)
    })

    private fun continueSearch(direction: Int) {
        judo.state[KEY_LAST_SEARCH_STRING]?.let {
            judo.searchForKeyword(it, direction)
            return
        }

        // TODO bell?
    }

    internal fun motionActionWithCount(motion: Motion): KeyAction =
        { _ -> applyMotion(repeat(motion, count.toRepeatCount())) }

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
        opMode.fullLineMotionKey = fullLineMotionKey
        withOperator(action)
    }


    private val input = MutableKeys()
    private val count = CountReadingBuffer()

    private var fromOpMode = false

    override fun onEnter() {
        input.clear()

        if (!fromOpMode) {
            buffer.cursor = maxOf(0, buffer.cursor - 1)
        }

        fromOpMode = false
    }

    override fun feedKey(key: KeyStroke, remap: Boolean, fromMap: Boolean) {
        if (key.keyCode == KeyEvent.VK_ENTER) {
            judo.send(buffer.toString(), fromMap)
            clearBuffer()
            return
        }

        // read in counts
        if (count.tryPush(key)) {
            // still reading...
            return
        }

        // handle key mappings
        if (tryMappings(key, remap, input, mapping, userMappings)) {
            // executed; clear the count
            count.clear()
            return
        }
    }

    override fun renderInputBuffer(): String = buffer.toString()
    override fun getCursor(): Int = buffer.cursor

    private fun clearBuffer() {
        count.clear()
        input.clear()
        buffer.clear()
        history.resetHistoryOffset()
    }

    private fun actionOnCount(motionFactory: (Int) -> Motion, step: Int, action: KeyActionOnRange): KeyAction {
        return { core ->
            val motionWithCount = motionFactory(step * count.toRepeatCount())
            actionOn(motionWithCount, action).invoke(core)
        }
    }

    private fun withCount(action: (Int) -> Unit): KeyAction =
        { _ -> action.invoke(count.toRepeatCount()) }
}


