package net.dhleong.judo.jline.stacks

import net.dhleong.judo.jline.IJLineWindow
import net.dhleong.judo.jline.JLineDisplay
import net.dhleong.judo.jline.JLineTabpage

/**
 * @author dhleong
 */
class RootStack(private var tabpage: JLineTabpage) : IStack {

    override val isHidden: Boolean = false

    override val parent: IStack = this
    lateinit var child: IStack

    override val width: Int
        get() = tabpage.width
    override val height: Int
        get() = tabpage.height

    override val lastResizeRequest: Long
        get() = child.lastResizeRequest

    override fun add(item: IStack) = throw UnsupportedOperationException()
    override fun getCollapseChild(): IStack? = null
    override fun stackWithWindow(predicate: (IJLineWindow) -> Boolean) =
        child.stackWithWindow(predicate)

    override fun render(display: JLineDisplay, x: Int, y: Int) =
        child.render(display, x, y)
    override fun getXPositionOf(window: IJLineWindow): Int =
        child.getXPositionOf(window)
    override fun getYPositionOf(window: IJLineWindow): Int =
        child.getYPositionOf(window)
    override fun remove(child: IStack) = throw UnsupportedOperationException()
    override fun replace(old: IStack, new: IStack) {
        if (old != child) throw IllegalArgumentException("old ($old) is not current ($child)")
        child = new
    }
    override fun resize(width: Int, height: Int) = child.resize(width, height)
    override fun nextWindow(): IJLineWindow = child.nextWindow()

    /*
        Window commands: our algorithm is bottom-up, so if this ever
        gets called, there are no more candidate windows, so these are
        all nops
     */

    override fun focusUp(search: CountingStackSearch) { }
    override fun focusDown(search: CountingStackSearch) { }
    override fun focusLeft(search: CountingStackSearch) { }
    override fun focusRight(search: CountingStackSearch) { }
}
