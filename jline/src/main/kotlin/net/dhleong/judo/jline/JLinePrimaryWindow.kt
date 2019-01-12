package net.dhleong.judo.jline

import net.dhleong.judo.StateMap
import net.dhleong.judo.inTransaction
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

    override fun render(display: JLineDisplay, x: Int, y: Int) {
        (outputWindow as IJLineWindow).render(display, x, y)
        (promptWindow as IJLineWindow).render(display, x, y + outputWindow.height)
    }
}
