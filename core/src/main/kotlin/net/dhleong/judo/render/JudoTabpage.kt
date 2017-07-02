package net.dhleong.judo.render

import net.dhleong.judo.StateMap

/**
 * @author dhleong
 */
class JudoTabpage(
    override val id: Int,
    private val settings: StateMap,
    initialWindow: IJudoWindow
) : IJudoTabpage {

    override var width: Int = initialWindow.width
    override var height: Int = initialWindow.height
    override var currentWindow: IJudoWindow = initialWindow

    private var nextWindowId = initialWindow.id + 1

    private val rootStack = RootStack(this).apply {
        child = WindowStack(this, initialWindow)
    }
    private var currentStack = rootStack.child

    override fun vsplit(percentage: Float, buffer: IJudoBuffer): IJudoWindow {
        TODO("not implemented")
    }

    override fun vsplit(cols: Int, buffer: IJudoBuffer): IJudoWindow {
        TODO("not implemented")
    }

    override fun hsplit(percentage: Float, buffer: IJudoBuffer): IJudoWindow {
        val containerHeight = currentStack.parent.height
        return hsplit(containerHeight * percentage, buffer)
    }

    override fun hsplit(cols: Int, buffer: IJudoBuffer): IJudoWindow {
        val thisStack = currentStack
        val parent = currentStack.parent
        val newWindow = JudoWindow(++nextWindowId, settings, parent.width, cols, buffer)

        if (parent is VerticalStack) {
            parent.add(WindowStack(parent, newWindow))
        } else {
            val newStack = VerticalStack(parent, width, height)
            newStack.add(thisStack)

            newStack.add(WindowStack(newStack, newWindow))
            parent.replace(thisStack, newStack)
        }

        return newWindow
    }

    override fun resize(width: Int, height: Int) =
        rootStack.resize(width, height)

    override fun getDisplayLines(lines: MutableList<CharSequence>) =
        rootStack.getDisplayLines(lines)
}

/**
 * Splits are implemented via Stacks, because it's more intuitive:
 * A vertical split contains windows arranged horizontally;
 * A vertical *stack* contains windows arranged vertically.
 */
internal interface IStack {
    val width: Int
    val height: Int
    val parent: IStack

    fun add(item: IStack)
    fun getDisplayLines(lines: MutableList<CharSequence>)
    fun replace(old: IStack, new: IStack)
    fun resize(width: Int, height: Int)
}

internal class RootStack(private var tabpage: JudoTabpage) : IStack {
    override val parent: IStack = this
    lateinit var child: IStack

    override val width: Int
        get() = tabpage.width
    override val height: Int
        get() = tabpage.height

    override fun add(item: IStack) = throw UnsupportedOperationException()
    override fun getDisplayLines(lines: MutableList<CharSequence>) =
        child.getDisplayLines(lines)
    override fun replace(old: IStack, new: IStack) {
        if (old != child) throw IllegalArgumentException("old is not current")
        child = new
    }
    override fun resize(width: Int, height: Int) = child.resize(width, height)
}

internal class WindowStack(
    override val parent: IStack,
    val window: IJudoWindow
) : IStack {
    override val width: Int
        get() = window.width

    override val height: Int
        get() = window.height

    override fun add(item: IStack) = throw UnsupportedOperationException()
    override fun replace(old: IStack, new: IStack) = throw UnsupportedOperationException()

    override fun getDisplayLines(lines: MutableList<CharSequence>) =
        window.getDisplayLines(lines)
    override fun resize(width: Int, height: Int) =
        window.resize(width, height)
}

internal abstract class BaseStack(
    override var parent: IStack,
    override var width: Int, override var height: Int
) : IStack {

    internal val contents = ArrayList<IStack>(4)

    override fun replace(old: IStack, new: IStack) {
        val oldIndex = contents.indexOf(old)
        if (oldIndex < 0) throw IllegalArgumentException("old stack not found")
        contents[oldIndex] = new
    }
}

internal class VerticalStack(parent: IStack, width: Int, height: Int)
    : BaseStack(parent, width, height) {

    override fun add(item: IStack) {
        val otherHeightDelta = item.height / contents.size
        for (row in contents) {
            row.resize(width, maxOf(2, row.height - otherHeightDelta))
        }

        contents.add(item)
        resize(width, height)
    }

    override fun getDisplayLines(lines: MutableList<CharSequence>) {
        // vertical stack is easy
        for (row in contents) {
            row.getDisplayLines(lines)
        }
    }

    override fun resize(width: Int, height: Int) {
        val rows = contents.size
        val separators = rows - 1
        var availableHeight = height - separators

        val last = contents.lastIndex
        for (i in contents.indices) {
            val item = contents[i]
            val requestedHeight = item.height
            val remainingRows = last - i
            val allottedHeight = maxOf(0,
                minOf(requestedHeight, availableHeight) -
                // leave room for the remaining rows
                2 * remainingRows
            )
            availableHeight -= allottedHeight

            item.resize(width, allottedHeight)
        }
    }

}
