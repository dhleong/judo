package net.dhleong.judo.jline.stacks

import net.dhleong.judo.jline.IJLineWindow
import net.dhleong.judo.jline.JLineDisplay

/**
 * @author dhleong
 */
class WindowStack(
    override val parent: IStack,
    val window: IJLineWindow
) : IStack {
    override val width: Int
        get() = window.width

    override val height: Int
        get() = window.height

    override fun add(item: IStack) = throw UnsupportedOperationException()
    override fun getCollapseChild(): IStack? = null
    override fun remove(child: IStack) = throw UnsupportedOperationException()
    override fun replace(old: IStack, new: IStack) = throw UnsupportedOperationException()

    override fun stackWithWindow(predicate: (IJLineWindow) -> Boolean): WindowStack? =
        if (predicate(window)) this
        else null

    override fun render(display: JLineDisplay, x: Int, y: Int) {
        window.render(display, x, y)
    }

    override fun getYPositionOf(window: IJLineWindow) =
        if (window == this.window) 0
        else -1
    override fun resize(width: Int, height: Int) =
        window.resize(width, height)
}
