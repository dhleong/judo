package net.dhleong.judo.jline

import net.dhleong.judo.StateMap
import net.dhleong.judo.WindowCommandHandler
import net.dhleong.judo.inTransaction
import net.dhleong.judo.jline.stacks.CountingStackSearch
import net.dhleong.judo.jline.stacks.IStack
import net.dhleong.judo.jline.stacks.RootStack
import net.dhleong.judo.jline.stacks.VerticalStack
import net.dhleong.judo.jline.stacks.WindowStack
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.PrimaryJudoWindow

/**
 * @author dhleong
 */
class JLineTabpage(
    private val renderer: JLineRenderer,
    private val ids: IdManager,
    private val settings: StateMap,
    private val initialWindow: IJLineWindow
) : IJudoTabpage, WindowCommandHandler {

    override val id: Int = ids.newTabpage()
    override var width: Int = initialWindow.width
    override var height: Int = initialWindow.height
    override var currentWindow: IJudoWindow
        get() = myCurrentWindow
        set(value) = renderer.inTransaction {
            currentStack = rootStack.stackWithWindow { it === value }
                ?: throw IllegalArgumentException(
                    "Window #${value.id} does not belong to Tabpage #$id"
                )
            myCurrentWindow.isFocused = false

            myCurrentWindow = value
            value.isFocused = true
        }

    private var myCurrentWindow: IJudoWindow = initialWindow

    private val rootStack = RootStack(this).apply {
        child = WindowStack(this, initialWindow)
    }
    private var currentStack: WindowStack = rootStack.child as WindowStack

    fun render(display: JLineDisplay) {
        rootStack.render(display, 0, 0)
    }

    override fun findWindowById(id: Int): IJudoWindow? =
        rootStack.stackWithWindow { it.id == id }?.window

    override fun findWindowByBufferId(id: Int): IJudoWindow? =
        rootStack.stackWithWindow { it.currentBuffer.id == id }?.window

    override fun vsplit(percentage: Float, buffer: IJudoBuffer): IJudoWindow {
        TODO("not implemented")
    }

    override fun vsplit(cols: Int, buffer: IJudoBuffer): IJudoWindow {
        TODO("not implemented")
    }

    override fun hsplit(percentage: Float, buffer: IJudoBuffer): IJudoWindow {
        val containerHeight = currentStack.parent.height
        return hsplit((containerHeight * percentage).toInt(), buffer)
    }

    override fun hsplit(rows: Int, buffer: IJudoBuffer): IJudoWindow {
        val thisStack = currentStack
        val parent = currentStack.parent
        val winHeight = rows + 1 // include room for a status line
        val newWindow = JLineWindow(renderer, ids, settings, parent.width, winHeight, buffer, isFocusable = true)

        renderer.inTransaction {
            if (parent is VerticalStack) {
                parent.add(WindowStack(parent, newWindow).also {
                    currentStack = it
                })
            } else {
                val newStack = VerticalStack(parent, width, height)
                thisStack.parent = newStack
                newStack.add(thisStack)

                newStack.add(WindowStack(newStack, newWindow).also {
                    currentStack = it
                })
                parent.replace(thisStack, newStack)
            }
            currentWindow = newWindow
        }

        return newWindow
    }

    override fun resize(width: Int, height: Int) = renderer.inTransaction {
        this.width = width
        this.height = height
        rootStack.resize(width, height)
    }

    override fun getYPositionOf(window: IJudoWindow): Int =
        rootStack.getYPositionOf(window as IJLineWindow)

    override fun unsplit() {
        rootStack.child = WindowStack(rootStack, initialWindow)
        rootStack.resize(width, height)
        // use [currentWindow] to ensure focus is moved as appropriate
        currentWindow = initialWindow
    }

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

        if (window.isFocused) {
            // TODO pick the new focused window
            currentWindow = stack.nextWindow()
        }
    }

    /*
        Window commands
     */

    override fun focusUp(count: Int) = focusWithCurrent(count) { focusUp(it) }
    override fun focusDown(count: Int) = focusWithCurrent(count) { focusDown(it) }

    private inline fun focusWithCurrent(
        count: Int,
        block: IStack.(CountingStackSearch) -> Unit
    ) {
        val search = CountingStackSearch(currentWindow as IJLineWindow, count)
        currentStack.block(search)
        if (search.current !== currentWindow) {
            search.current?.let { result ->
                renderer.inTransaction {
                    currentWindow = result
                }
            }
        }
    }

}