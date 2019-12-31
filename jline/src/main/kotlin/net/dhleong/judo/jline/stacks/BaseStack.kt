package net.dhleong.judo.jline.stacks

import net.dhleong.judo.jline.IJLineWindow

/**
 * @author dhleong
 */
abstract class BaseStack(
    override var parent: IStack,
    override var width: Int,
    override var height: Int
) : IStack {

    override val lastResizeRequest: Long
        get() = contents.maxBy {
            it.lastResizeRequest
        }?.lastResizeRequest ?: 0L

    override val isHidden: Boolean
        get() = contents.all { it.isHidden }

    internal val contents = ArrayList<IStack>(4)

    internal val visibleContentsCount: Int
        get() = contents.sumBy { if (it.isHidden) 0 else 1 }

    override fun getCollapseChild(): IStack? {
        if (contents.size == 1) {
            return contents[0]
        }

        return null
    }

    override fun nextWindow(): IJLineWindow = contents.first().nextWindow()

    override fun stackWithWindow(predicate: (IJLineWindow) -> Boolean): WindowStack? =
        contents.asSequence()
            .map { it.stackWithWindow(predicate) }
            .firstOrNull { it != null }

    override fun remove(child: IStack) {
        if (!contents.remove(child)) {
            throw IllegalArgumentException("$child not contained in $this")
        }
    }

    override fun replace(old: IStack, new: IStack) {
        val oldIndex = contents.indexOf(old)
        if (oldIndex < 0) throw IllegalArgumentException("old stack not found")
        contents[oldIndex] = new
    }

    internal inline fun doResize(
        width: Int, height: Int,
        available: Int,
        minDimension: Int,
        getDimension: (IStack) -> Int,
        setDimension: IStack.(Int) -> Unit
    ) {
        this.width = width
        this.height = height
        val itemsCount = visibleContentsCount
        var freeSpace = available

        var remainingItems = itemsCount - 1
        for (item in contents.filter { !it.isHidden }.sortedByDescending { it.lastResizeRequest }) {
            val requested = getDimension(item)
            val allotted =
                if (remainingItems == 0) freeSpace
                else maxOf(
                    minDimension,
                    minOf(
                        requested,
                        // leave room for the remaining items
                        freeSpace - minDimension * remainingItems
                    )
                )
            freeSpace -= allotted

            item.setDimension(allotted)
            --remainingItems
        }
    }

    /*
        Window commands
     */

    override fun focusUp(search: CountingStackSearch) = defaultFocus(search) { focusUp(search) }
    override fun focusDown(search: CountingStackSearch) = defaultFocus(search) { focusDown(search) }
    override fun focusLeft(search: CountingStackSearch) = defaultFocus(search) { focusLeft(search) }
    override fun focusRight(search: CountingStackSearch) = defaultFocus(search) { focusRight(search) }

    protected fun focus(
        search: CountingStackSearch,
        direction: Int,
        next: IStack.(search: CountingStackSearch) -> Unit
    ) {
        val oldIndex = search.current?.let { currentWindow ->
            val stack = childStackWithWindow(currentWindow)
            contents.indexOf(stack)
        } ?: when (direction) {
            1 -> 0
            -1 -> contents.lastIndex
            else -> throw IllegalStateException()
        }
        if (oldIndex == -1) throw IllegalStateException("Couldn't find an appropriate child stack")

        val newIndex = oldIndex + search.count * direction
        when {
            newIndex < 0 -> {
                val newCount = -newIndex
                if (newCount == search.count) {
                    // nothing more to consume
                    parent.next(search)
                    return
                }
                search.count = newCount
                contents.first().next(search)
            }

            newIndex >= contents.size -> {
                val newCount = newIndex - contents.size
                if (newCount == search.count) {
                    // nothing more to consume
                    parent.next(search)
                    return
                }

                search.count = newCount
                contents.last().next(search)
            }

            else -> {
                search.count = 0
                contents[newIndex].next(search)
            }
        }
    }

    private inline fun defaultFocus(
        search: CountingStackSearch,
        next: StackWindowCommandHandler.(search: CountingStackSearch) -> Unit
    ) {
        if (search.count <= 0) {
            contents.first().next(search)
        } else {
            parent.next(search)
        }
    }

}
