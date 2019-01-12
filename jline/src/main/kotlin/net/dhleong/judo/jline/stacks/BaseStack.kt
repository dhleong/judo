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

    override fun stackWithWindow(predicate: (IJLineWindow) -> Boolean): WindowStack? =
        contents.asSequence()
            .map { it.stackWithWindow(predicate) }
            .firstOrNull { it != null }

    override fun replace(old: IStack, new: IStack) {
        val oldIndex = contents.indexOf(old)
        if (oldIndex < 0) throw IllegalArgumentException("old stack not found")
        contents[oldIndex] = new
    }
}
