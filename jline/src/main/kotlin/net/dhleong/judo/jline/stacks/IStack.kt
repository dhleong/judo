package net.dhleong.judo.jline.stacks

import net.dhleong.judo.jline.IJLineWindow
import net.dhleong.judo.jline.JLineDisplay

/**
 * Splits are implemented via Stacks, because it's more intuitive:
 * A vertical split contains windows arranged horizontally;
 * A vertical *stack* contains windows arranged vertically.
 *
 * @author dhleong
 */
interface IStack {
    val width: Int
    val height: Int
    val parent: IStack

    fun add(item: IStack)
    fun stackWithWindow(predicate: (IJLineWindow) -> Boolean): WindowStack?

    /**
     * If we have just a single child that can be collapsed
     * into our parent, return it here
     */
    fun getCollapseChild(): IStack?

    fun render(display: JLineDisplay, x: Int, y: Int)
    fun getYPositionOf(window: IJLineWindow): Int
    fun remove(child: IStack)
    fun replace(old: IStack, new: IStack)
    fun resize(width: Int, height: Int)
}

