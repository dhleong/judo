package net.dhleong.judo.modes

import net.dhleong.judo.CursorType
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.Mode
import net.dhleong.judo.OperatorFunc
import net.dhleong.judo.inTransaction
import net.dhleong.judo.input.CountReadingBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.KeyAction
import net.dhleong.judo.input.KeyMapHelper
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.action
import net.dhleong.judo.input.keys
import net.dhleong.judo.modes.output.OutputBufferCharSequence
import net.dhleong.judo.motions.ALL_MOTIONS
import net.dhleong.judo.motions.LINEWISE_MOTIONS
import net.dhleong.judo.motions.Motion
import net.dhleong.judo.motions.repeat

/**
 * @author dhleong
 */
class OutputNormalMode(
    private val judo: IJudoCore
) : Mode {

    override val name: String = "o-normal"

    private lateinit var buffer: OutputBufferCharSequence

    private val mapping = KeyMapping(listOf(
        keys("i") to { core ->
            core.exitMode()
            core.feedKeys("i")
        },
        keys("I") to { core ->
            core.exitMode()
            core.feedKeys("I")
        },

        keys("y") to action {
            // TODO bell on error?
            withOperator('y') { range ->
                judo.registers.current.value =
                    buffer.get(range).toString()
            }
        },
        keys("Y") to action {
//            judo.registers.current.value = buffer.toString()
            TODO("yank full line")
        },

        keys("<ctrl b>") to { core -> core.scrollPages(1) },
        keys("<ctrl f>") to { core -> core.scrollPages(-1) }
    ) + (ALL_MOTIONS + LINEWISE_MOTIONS).filter { (_, motion) ->
        // text object motions can't be used as an action
        !motion.isTextObject
    }.map { (keys, motion) ->
        keys to motionActionWithCount(motion)
    })

    private val count = CountReadingBuffer()

    private var fromOpMode = false

    private val keymaps = KeyMapHelper(judo, mapping)

    override fun onEnter() {
        if (!fromOpMode) {
            val win = judo.tabpage.currentWindow
            buffer = OutputBufferCharSequence(win)
        }
        fromOpMode = false

        judo.renderer.inTransaction {
            val win = judo.renderer.currentTabpage.currentWindow
            win.apply {
                isOutputFocused = true
                cursorLine = 0
                cursorCol = 0

                // TODO put it at the end of the line? we'd have to
                //  be able to ask the window for *rendered lines*
            }
            judo.setCursorType(CursorType.BLOCK)
        }
    }

    override fun onExit() {
        judo.renderer.currentTabpage.currentWindow.isOutputFocused = false
    }

    override suspend fun feedKey(key: Key, remap: Boolean, fromMap: Boolean) {
        if (count.tryPush(key)) {
            return
        }

        if (keymaps.tryMappings(key, remap)) {
            return
        }
    }

    private fun motionActionWithCount(motion: Motion): KeyAction =
        { applyMotion(repeat(motion, count.toRepeatCount())) }

    private suspend fun applyMotion(motion: Motion) {
        val win = judo.tabpage.currentWindow

        motion.applyTo(judo, buffer)

        buffer.applyCursorTo(win)
    }

    @Suppress("SameParameterValue")
    private fun withOperator(fullLineMotionKey: Char, action: OperatorFunc) {
        withOperator(action, fullLineMotionKey)
    }

    private fun withOperator(action: OperatorFunc, fullLineMotionKey: Char? = null) {
        fromOpMode = true
        withOperator(judo, count, buffer, action) {
            val mode = OperatorPendingMode(judo, buffer, includeLinewiseMotions = true)
            if (fullLineMotionKey != null) {
                mode.fullLineMotionKey = fullLineMotionKey
            }
            enterMode(mode)
        }
    }

}