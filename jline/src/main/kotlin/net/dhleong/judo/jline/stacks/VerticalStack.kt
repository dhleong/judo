package net.dhleong.judo.jline.stacks

import net.dhleong.judo.jline.IJLineWindow
import net.dhleong.judo.jline.JLineDisplay

const val WINDOW_MIN_HEIGHT = 2

/**
 * @author dhleong
 */
class VerticalStack(
    parent: IStack,
    width: Int,
    height: Int
) : BaseStack(parent, width, height) {

    override fun add(item: IStack) {
        if (contents.isNotEmpty()) {
            var available = height
            for (row in contents) {
                available -= row.height
            }

            // reduce the size of other buffers to make room for the new item
            if (available < item.height) {
                val otherHeightDelta = (item.height - available) / visibleContentsCount.coerceAtLeast(1)
                for (row in contents) {
                    row.resize(width, maxOf(
                        WINDOW_MIN_HEIGHT,
                        row.height - otherHeightDelta
                    ))
                }
            }
        }

        // insert at the top
        // TODO vim has settings (and function args) for where to insert the new window...
        contents.add(0, item)
        resize(width, height)
    }

    override fun render(display: JLineDisplay, x: Int, y: Int) {
        // vertical stack is easy
        var line = y
        for (i in contents.indices) {
            val row = contents[i]

            row.render(display, x, line)
            line += row.height
        }
    }

    override fun getXPositionOf(window: IJLineWindow): Int {
        for (row in contents) {
            val rowX = row.getXPositionOf(window)
            if (rowX != -1) return rowX
        }

        // not in this stack
        return -1
    }

    override fun getYPositionOf(window: IJLineWindow): Int {
        var yOffset = 0
        for (row in contents) {
            val rowY = row.getYPositionOf(window)
            if (rowY != -1) return yOffset + rowY

            yOffset += row.height
        }

        // not in this stack
        return -1
    }

    override fun resize(width: Int, height: Int) = doResize(
        width, height,
        available = height,
        minDimension = WINDOW_MIN_HEIGHT,
        getDimension = { it.height },
        setDimension = { h -> resize(width, h) }
    )

    override fun focusUp(search: CountingStackSearch) = focus(search, -1, IStack::focusUp)
    override fun focusDown(search: CountingStackSearch) = focus(search, 1, IStack::focusDown)

}
