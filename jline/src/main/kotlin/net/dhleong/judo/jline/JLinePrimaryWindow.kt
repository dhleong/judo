package net.dhleong.judo.jline

import net.dhleong.judo.StateMap
import net.dhleong.judo.inTransaction
import net.dhleong.judo.render.IJudoBuffer2
import net.dhleong.judo.render.IJudoWindow2
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.PrimaryJudoWindow2

/**
 * @author dhleong
 */
class JLinePrimaryWindow(
    private val renderer: JLineRenderer2,
    ids: IdManager,
    settings: StateMap,
    outputBuffer: IJudoBuffer2,
    initialWidth: Int,
    initialHeight: Int
) : PrimaryJudoWindow2(
    ids,
    settings,
    outputBuffer,
    initialWidth,
    initialHeight
), IJLineWindow {

    override fun createBuffer(ids: IdManager): IJudoBuffer2 = renderer.createBuffer()

    override fun createWindow(
        ids: IdManager,
        settings: StateMap,
        initialWidth: Int,
        initialHeight: Int,
        initialBuffer: IJudoBuffer2,
        isFocusable: Boolean,
        statusLineOverlaysOutput: Boolean
    ): IJudoWindow2 = JLineWindow(
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
