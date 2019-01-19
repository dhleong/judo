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

    internal val contents = ArrayList<IStack>(4)

    override fun nextWindow(): IJLineWindow = contents.first().nextWindow()

    override fun stackWithWindow(predicate: (IJLineWindow) -> Boolean): WindowStack? =
        contents.asSequence()
            .map { it.stackWithWindow(predicate) }
            .firstOrNull { it != null }

    override fun replace(old: IStack, new: IStack) {
        val oldIndex = contents.indexOf(old)
        if (oldIndex < 0) throw IllegalArgumentException("old stack not found")
        contents[oldIndex] = new
    }

    /*
        Window commands
     */

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

}
