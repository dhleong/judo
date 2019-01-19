package net.dhleong.judo.jline

import net.dhleong.judo.StateMap
import net.dhleong.judo.inTransaction
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.PrimaryJudoWindow

/**
 * @author dhleong
 */
class JLinePrimaryWindow(
    private val renderer: JLineRenderer,
    ids: IdManager,
    settings: StateMap,
    outputBuffer: IJudoBuffer,
    initialWidth: Int,
    initialHeight: Int
) : PrimaryJudoWindow(
    ids,
    settings,
    outputBuffer,
    initialWidth,
    initialHeight
), IJLineWindow {

    override fun createBuffer(ids: IdManager): IJudoBuffer = renderer.createBuffer()

    override fun createWindow(
        ids: IdManager,
        settings: StateMap,
        initialWidth: Int,
        initialHeight: Int,
        initialBuffer: IJudoBuffer,
        isFocusable: Boolean,
        statusLineOverlaysOutput: Boolean
    ): IJudoWindow = JLineWindow(
        renderer,
        ids,
        settings,
        initialWidth,
        initialHeight,
        initialBuffer,
        isFocusable,
        statusLineOverlaysOutput
    )

    override fun resize(width: Int, height: Int) = renderer.inTransaction {
        super.resize(width, height)
    }

    override fun echo(text: FlavorableCharSequence) = (promptWindow as IJLineWindow).echo(text)
    override fun clearEcho() = (promptWindow as IJLineWindow).clearEcho()

    override fun render(display: JLineDisplay, x: Int, y: Int) {
        (outputWindow as IJLineWindow).render(display, x, y)
        (promptWindow as IJLineWindow).render(display, x, y + outputWindow.height)
    }

    override fun scrollLines(count: Int) = clearingEcho { outputWindow.scrollLines(count) }
    override fun scrollPages(count: Int) = clearingEcho { outputWindow.scrollPages(count) }
    override fun scrollToBottom() = clearingEcho { outputWindow.scrollToBottom() }
    override fun scrollToBufferLine(line: Int, offsetOnLine: Int) = clearingEcho {
        outputWindow.scrollToBufferLine(line, offsetOnLine)
    }
    override fun searchForKeyword(word: CharSequence, direction: Int) = clearingEcho {
        outputWindow.searchForKeyword(word, direction)
    }

    private inline fun <R> clearingEcho(block: () -> R) = renderer.inTransaction {
        (promptWindow as IJLineWindow).clearEcho()
        block()
    }
}
