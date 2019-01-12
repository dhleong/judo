package net.dhleong.judo.jline

import net.dhleong.judo.StateMap
import net.dhleong.judo.inTransaction
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
) : IJudoTabpage {

    override val id: Int = ids.newTabpage()
    override var width: Int = initialWindow.width
    override var height: Int = initialWindow.height
    override var currentWindow: IJudoWindow = initialWindow

    private val rootStack = RootStack(this).apply {
        child = WindowStack(this, initialWindow)
    }
    private var currentStack = rootStack.child

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
        return hsplit(containerHeight * percentage, buffer)
    }

    override fun hsplit(rows: Int, buffer: IJudoBuffer): IJudoWindow {
        val thisStack = currentStack
        val parent = currentStack.parent
        val newWindow = JLineWindow(renderer, ids, settings, parent.width, rows, buffer)

        renderer.inTransaction {
            if (parent is VerticalStack) {
                parent.add(WindowStack(parent, newWindow))
            } else {
                val newStack = VerticalStack(parent, width, height)
                newStack.add(thisStack)

                newStack.add(WindowStack(newStack, newWindow))
                parent.replace(thisStack, newStack)
            }
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
        val newStack = WindowStack(rootStack, initialWindow)
        currentStack = newStack
        currentWindow = initialWindow
        rootStack.child = newStack
        rootStack.resize(width, height)
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
    }
}