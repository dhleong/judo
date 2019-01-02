package net.dhleong.judo.jline

import net.dhleong.judo.StateMap
import net.dhleong.judo.render.*

/**
 * @author dhleong
 */
class JLinePrimaryWindow(
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
) {
    override fun createBuffer(ids: IdManager): IJudoBuffer2 = JudoBuffer2(ids)

    override fun createWindow(
        ids: IdManager,
        settings: StateMap,
        initialWidth: Int,
        initialHeight: Int,
        initialBuffer: IJudoBuffer2,
        isFocusable: Boolean,
        statusLineOverlaysOutput: Boolean
    ): IJudoWindow2 = JLineWindow(
        ids,
        settings,
        initialWidth,
        initialHeight,
        initialBuffer,
        isFocusable,
        statusLineOverlaysOutput
    )

}
