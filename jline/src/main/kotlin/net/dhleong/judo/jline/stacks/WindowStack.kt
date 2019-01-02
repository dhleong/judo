package net.dhleong.judo.jline.stacks

import net.dhleong.judo.jline.JLineDisplay
import net.dhleong.judo.jline.JLineWindow

/**
 * @author dhleong
 */
class WindowStack(
    override val parent: IStack,
    val window: JLineWindow
) : IStack {
    override val width: Int
        get() = window.width

    override val height: Int
        get() = window.height

    override fun add(item: IStack) = throw UnsupportedOperationException()
    override fun getCollapseChild(): IStack? = null
    override fun remove(child: IStack) = throw UnsupportedOperationException()
    override fun replace(old: IStack, new: IStack) = throw UnsupportedOperationException()

    override fun stackWithWindow(predicate: (JLineWindow) -> Boolean): WindowStack? =
        if (predicate(window)) this
        else null

    override fun render(display: JLineDisplay, x: Int, y: Int) {
        window.render(display, x, y)
    }

    override fun getYPositionOf(window: JLineWindow) =
        if (window == this.window) 0
        else -1
    override fun resize(width: Int, height: Int) =
        window.resize(width, height)
}
