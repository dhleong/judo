package net.dhleong.judo.render

import net.dhleong.judo.StateMap
import org.jline.utils.AttributedString

val WINDOW_MIN_HEIGHT = 2

/**
 * @author dhleong
 */
class JudoTabpage(
    private val ids: IdManager,
    private val settings: StateMap,
    private val initialWindow: IJudoWindow
) : IJudoTabpage {

    override var id = ids.newTabpage()
    override var width: Int = initialWindow.width
    override var height: Int = initialWindow.height
    override var currentWindow: IJudoWindow = initialWindow

    private val rootStack = RootStack(this).apply {
        child = WindowStack(this, initialWindow)
    }
    private var currentStack = rootStack.child

    override fun close(window: IJudoWindow) {
        if (window is PrimaryJudoWindow) {
            throw IllegalArgumentException("You cannot close the primary window!")
        }

        val winStack = rootStack.stackWithWindow { it == window }
            ?: throw IllegalArgumentException("No such window on this tabpage")

        var stack = winStack.parent
        stack.remove(winStack)

        while (stack.parent != stack) {
            val collapseChild = stack.getCollapseChild()
                ?: break // nothing else

            stack.parent.replace(stack, collapseChild)
            stack = stack.parent
        }

        resize(width, height)
    }

    override fun findWindowById(id: Int) =
        rootStack.stackWithWindow { it.id == id }?.window

    override fun findWindowByBufferId(id: Int) =
        rootStack.stackWithWindow { it.currentBuffer.id == id }?.window

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

    override fun hsplit(rows: Int, buffer: IJudoBuffer): IJudoWindow {
        val thisStack = currentStack
        val parent = currentStack.parent
        val newWindow = JudoWindow(ids, settings, parent.width, rows, buffer)

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

    override fun resize() = resize(width, height)
    override fun resize(width: Int, height: Int) {
        rootStack.resize(width, height)
        this.width = width
        this.height = height
    }

    override fun unsplit() {
        val newStack = WindowStack(rootStack, initialWindow)
        currentStack = newStack
        currentWindow = initialWindow
        rootStack.child = newStack
        rootStack.resize(width, height)
    }

    override fun getDisplayLines(lines: MutableList<CharSequence>) =
        rootStack.getDisplayLines(lines)

    override fun getYPositionOf(window: IJudoWindow): Int =
        rootStack.getYPositionOf(window)
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
    fun stackWithWindow(predicate: (IJudoWindow) -> Boolean): WindowStack?

    /**
     * If we have just a single child that can be collapsed
     * into our parent, return it here
     */
    fun getCollapseChild(): IStack?

    fun getDisplayLines(lines: MutableList<CharSequence>)
    fun getYPositionOf(window: IJudoWindow): Int
    fun remove(child: IStack)
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
    override fun getCollapseChild(): IStack? = null
    override fun stackWithWindow(predicate: (IJudoWindow) -> Boolean) =
        child.stackWithWindow(predicate)
    override fun getDisplayLines(lines: MutableList<CharSequence>) =
        child.getDisplayLines(lines)
    override fun getYPositionOf(window: IJudoWindow): Int =
        child.getYPositionOf(window)
    override fun remove(child: IStack) = throw UnsupportedOperationException()
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
    override fun getCollapseChild(): IStack? = null
    override fun remove(child: IStack) = throw UnsupportedOperationException()
    override fun replace(old: IStack, new: IStack) = throw UnsupportedOperationException()

    override fun stackWithWindow(predicate: (IJudoWindow) -> Boolean): WindowStack? =
        if (predicate(window)) this
        else null

    override fun getDisplayLines(lines: MutableList<CharSequence>) {
        val old = lines.size
        window.getDisplayLines(lines)
        val added = lines.size - old
        if (added != height) {
            throw IllegalStateException("$window created $added lines but had height $height")
        }
    }

    override fun getYPositionOf(window: IJudoWindow) =
        if (window == this.window) 0
        else -1
    override fun resize(width: Int, height: Int) =
        window.resize(width, height)
}

internal abstract class BaseStack(
    override var parent: IStack,
    override var width: Int, override var height: Int
) : IStack {

    internal val contents = ArrayList<IStack>(4)

    override fun stackWithWindow(predicate: (IJudoWindow) -> Boolean): WindowStack? =
        contents.asSequence()
            .map { it.stackWithWindow(predicate) }
            .firstOrNull { it != null }

    override fun replace(old: IStack, new: IStack) {
        val oldIndex = contents.indexOf(old)
        if (oldIndex < 0) throw IllegalArgumentException("old stack not found")
        contents[oldIndex] = new
    }
}

internal class VerticalStack(parent: IStack, width: Int, height: Int)
    : BaseStack(parent, width, height) {

    private var cachedSeparator: AttributedString? = null

    override fun add(item: IStack) {
        if (contents.isNotEmpty()) {
            var available = height
            for (row in contents) {
                available -= row.height
            }

            // reduce the size of other buffers to make room for the new item
            if (available < item.height) {
                val otherHeightDelta = (item.height - available) / contents.size
                for (row in contents) {
                    row.resize(width, maxOf(
                        WINDOW_MIN_HEIGHT,
                        row.height - otherHeightDelta
                    ))
                }
            }
        }

        // insert at the top
        contents.add(0, item)
        resize(width, height)
    }

    override fun getCollapseChild(): IStack? {
        if (contents.size == 1) {
            return contents[0]
        }

        return null
    }

    override fun getDisplayLines(lines: MutableList<CharSequence>) {
        // vertical stack is easy
        for (i in contents.indices) {
            if (i > 0) {
                // separator
                lines.add(getSeparator())
            }

            val row = contents[i]
            row.getDisplayLines(lines)
        }
    }

    override fun getYPositionOf(window: IJudoWindow): Int {
        var yOffset = 0
        for (row in contents) {
            val rowY = row.getYPositionOf(window)
            if (rowY != -1) return yOffset + rowY

            yOffset += row.height + 1 // +1 for separator
        }

        // not in this stack
        return -1
    }

    override fun remove(child: IStack) {
        if (!contents.remove(child)) {
            throw IllegalArgumentException("$child not contained in $this")
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
            val allottedHeight =
                if (remainingRows == 0) availableHeight
                else maxOf(
                    WINDOW_MIN_HEIGHT,
                    minOf(requestedHeight,
                        // leave room for the remaining rows
                        availableHeight - WINDOW_MIN_HEIGHT * remainingRows)
                )
            availableHeight -= allottedHeight

            item.resize(width, allottedHeight)
        }
    }

    private fun getSeparator(): AttributedString {
        cachedSeparator?.let {
            if (it.length == width) return it
        }

        // TODO fancy separator?
        val newSeparator = AttributedString.fromAnsi("-".repeat(width))
        cachedSeparator = newSeparator
        return newSeparator
    }
}
