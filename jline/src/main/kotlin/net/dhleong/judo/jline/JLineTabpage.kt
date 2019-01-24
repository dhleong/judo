package net.dhleong.judo.jline

import net.dhleong.judo.JudoRendererEvent
import net.dhleong.judo.StateMap
import net.dhleong.judo.WindowCommandHandler
import net.dhleong.judo.inTransaction
import net.dhleong.judo.jline.stacks.CountingStackSearch
import net.dhleong.judo.jline.stacks.HorizontalStack
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
        val containerWidth = currentStack.parent.width
        return vsplit((containerWidth * percentage).toInt(), buffer)
    }

    override fun vsplit(cols: Int, buffer: IJudoBuffer): IJudoWindow =
        splitInto(
            cols,
            currentStack.parent.height,
            buffer,
            ::HorizontalStack
        )

    override fun hsplit(percentage: Float, buffer: IJudoBuffer): IJudoWindow {
        val containerHeight = currentStack.parent.height
        return hsplit((containerHeight * percentage).toInt(), buffer)
    }

    override fun hsplit(rows: Int, buffer: IJudoBuffer): IJudoWindow =
        splitInto(
            currentStack.parent.width,
            rows + 1, // include room for a status line
            buffer,
            ::VerticalStack
        )

    private inline fun <reified T : IStack> splitInto(
        windowWidth: Int,
        windowHeight: Int,
        buffer: IJudoBuffer,
        createStack: (parent: IStack, width: Int, height: Int) -> T
    ): IJudoWindow = renderer.inTransaction {
        val thisStack = currentStack
        val parent = currentStack.parent
        val newWindow = createWindow(windowWidth, windowHeight, buffer)

        if (parent is T) {
            parent.add(WindowStack(parent, newWindow))
        } else {
            val newStack = createStack(parent, thisStack.width, thisStack.height)
            renderer.inWindowResize {
                thisStack.parent = newStack
                newStack.add(thisStack)

                newStack.add(WindowStack(newStack, newWindow))
                parent.replace(thisStack, newStack)
            }
        }
        currentWindow = newWindow

        renderer.dispatch(JudoRendererEvent.OnLayout)
        return newWindow
    }

    override fun resize(width: Int, height: Int) = renderer.inTransaction {
        this.width = width
        this.height = height
        rootStack.resize(width, height)
    }

    fun getXPositionOf(window: IJudoWindow): Int =
        rootStack.getXPositionOf(window as IJLineWindow)

    override fun getYPositionOf(window: IJudoWindow): Int =
        rootStack.getYPositionOf(window as IJLineWindow)

    override fun unsplit() {
        rootStack.child = WindowStack(rootStack, initialWindow)
        rootStack.resize(width, height)
        // use [currentWindow] to ensure focus is moved as appropriate
        currentWindow = initialWindow
        renderer.dispatch(JudoRendererEvent.OnLayout)
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
        renderer.dispatch(JudoRendererEvent.OnLayout)
    }

    /*
        Window commands
     */

    override fun focusUp(count: Int) = focusWithCurrent(count) { focusUp(it) }
    override fun focusDown(count: Int) = focusWithCurrent(count) { focusDown(it) }
    override fun focusLeft(count: Int) = focusWithCurrent(count) { focusLeft(it) }
    override fun focusRight(count: Int) = focusWithCurrent(count) { focusRight(it) }

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

    private fun createWindow(width: Int, height: Int, buffer: IJudoBuffer) =
        JLineWindow(renderer, ids, settings, width, height, buffer, isFocusable = true)
}
