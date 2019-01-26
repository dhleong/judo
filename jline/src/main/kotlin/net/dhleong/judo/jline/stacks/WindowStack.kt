package net.dhleong.judo.jline.stacks

import net.dhleong.judo.jline.IJLineWindow
import net.dhleong.judo.jline.JLineDisplay

/**
 * @author dhleong
 */
class WindowStack(
    override var parent: IStack,
    val window: IJLineWindow
) : IStack {
    override val width: Int
        get() = window.width

    override val height: Int
        get() = window.height

    override val lastResizeRequest: Long
        get() = window.lastResizeRequest

    override fun add(item: IStack) = throw UnsupportedOperationException()
    override fun getCollapseChild(): IStack? = null
    override fun remove(child: IStack) = throw UnsupportedOperationException()
    override fun replace(old: IStack, new: IStack) = throw UnsupportedOperationException()

    override fun nextWindow(): IJLineWindow = window

    override fun stackWithWindow(predicate: (IJLineWindow) -> Boolean): WindowStack? =
        if (predicate(window)) this
        else null

    override fun render(display: JLineDisplay, x: Int, y: Int) {
        window.render(display, x, y)
    }

    override fun getXPositionOf(window: IJLineWindow) =
        if (window == this.window) 0
        else -1
    override fun getYPositionOf(window: IJLineWindow) =
        if (window == this.window) 0
        else -1
    override fun resize(width: Int, height: Int) =
        window.resize(width, height)

    /*
        Window commands
     */

    override fun focusUp(search: CountingStackSearch) = thisOrOnParent(search) { focusUp(search) }
    override fun focusDown(search: CountingStackSearch) = thisOrOnParent(search) { focusDown(search) }
    override fun focusLeft(search: CountingStackSearch) = thisOrOnParent(search) { focusLeft(search) }
    override fun focusRight(search: CountingStackSearch) = thisOrOnParent(search) { focusRight(search) }

    private inline fun thisOrOnParent(
        search: CountingStackSearch,
        block: IStack.(CountingStackSearch) -> Unit
    ) {
        if (0 == search.count) {
            search.current = window
        } else {
            parent.block(search)
        }
    }
}
