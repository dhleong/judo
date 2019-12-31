package net.dhleong.judo.jline.stacks

import net.dhleong.judo.jline.IJLineWindow
import net.dhleong.judo.jline.JLineDisplay
import net.dhleong.judo.jline.toAttributedStyle
import net.dhleong.judo.render.flavor.Flavor
import org.jline.utils.AttributedString

const val WINDOW_MIN_WIDTH = 2

/**
 * @author dhleong
 */
class HorizontalStack(
    parent: IStack,
    width: Int,
    height: Int
) : BaseStack(parent, width, height) {

    override fun add(item: IStack) {
        if (contents.isNotEmpty()) {
            // -1 for each separator (and an extra for the new item)
            var available = width - visibleContentsCount
            for (row in contents) {
                available -= row.width
            }

            // reduce the size of other buffers to make room for the new item
            if (available < item.width) {
                val otherWidthDelta = (item.width - available) / visibleContentsCount.coerceAtLeast(1)
                for (row in contents) {
                    row.resize(maxOf(
                        WINDOW_MIN_WIDTH,
                        row.width - otherWidthDelta
                    ), height)
                }
            }
        }

        // insert at the right
        // TODO vim has settings (and function args) for where to insert the new window...
        contents.add(item)
        resize(width, height)
    }

    override fun render(display: JLineDisplay, x: Int, y: Int) {
        // horizontal stack needs separators
        var col = x
        var i = 0
        for (win in contents) {
            if (win.isHidden) continue
            if (i++ > 0) {
                display.renderSeparator(x + col, y, height)
                ++col
            }

            win.render(display, col, y)
            col += win.width
        }
    }

    override fun getXPositionOf(window: IJLineWindow): Int {
        var xOffset = 0
        for (row in contents) {
            val rowX = row.getXPositionOf(window)
            if (rowX != -1) return xOffset + rowX

            xOffset += row.width + 1 // +1 for separator
        }

        // not in this stack
        return -1
    }

    override fun getYPositionOf(window: IJLineWindow): Int {
        for (row in contents) {
            val rowX = row.getYPositionOf(window)
            if (rowX != -1) return rowX
        }

        // not in this stack
        return -1
    }

    override fun resize(width: Int, height: Int) = doResize(
        width, height,
        available = width - (visibleContentsCount - 1).coerceAtLeast(0), // leave room for separators
        minDimension = WINDOW_MIN_WIDTH,
        getDimension = { it.width },
        setDimension = { w -> resize(w, height) }
    )

    override fun focusLeft(search: CountingStackSearch) = focus(search, -1, IStack::focusLeft)
    override fun focusRight(search: CountingStackSearch) = focus(search, 1, IStack::focusRight)

}

private fun JLineDisplay.renderSeparator(x: Int, y: Int, height: Int) {
    val flavor = Flavor.default.toAttributedStyle()
    val string = AttributedString(" ", flavor)
    for (i in y until y + height) {
        withLine(x, i) {
            append(string)
        }
    }
}

