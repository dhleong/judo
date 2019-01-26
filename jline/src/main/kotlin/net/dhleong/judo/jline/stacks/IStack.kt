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
interface IStack : StackWindowCommandHandler {
    val width: Int
    val height: Int
    val parent: IStack
    val lastResizeRequest: Long

    fun add(item: IStack)
    fun stackWithWindow(predicate: (IJLineWindow) -> Boolean): WindowStack?
    fun childStackWithWindow(win: IJLineWindow): IStack {
        var stack: IStack = stackWithWindow { it === win }
            ?: throw IllegalStateException("Window not contained in this stack")

        while (stack.parent !== this) {
            if (stack.parent === stack) {
                throw IllegalStateException()
            }

            stack = stack.parent
        }

        return stack
    }


    /**
     * If we have just a single child that can be collapsed
     * into our parent, return it here
     */
    fun getCollapseChild(): IStack?

    fun render(display: JLineDisplay, x: Int, y: Int)
    fun getXPositionOf(window: IJLineWindow): Int
    fun getYPositionOf(window: IJLineWindow): Int
    fun remove(child: IStack)
    fun replace(old: IStack, new: IStack)
    fun resize(width: Int, height: Int)

    /**
     * Pick the "next" window in this Stack. Used, for example,
     * to pick the new "focused" window after closing the previous
     */
    fun nextWindow(): IJLineWindow
}

