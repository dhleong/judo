package net.dhleong.judo.jline.stacks

import net.dhleong.judo.jline.IJLineWindow
import net.dhleong.judo.jline.JLineDisplay
import net.dhleong.judo.jline.toAttributedStyle
import net.dhleong.judo.render.Flavor
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
            var available = width - contents.size
            for (row in contents) {
                available -= row.width
            }

            // reduce the size of other buffers to make room for the new item
            if (available < item.width) {
                val otherWidthDelta = (item.width - available) / contents.size
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
        for (i in contents.indices) {
            if (i > 0) {
                display.renderSeparator(x + col, y, height)
                ++col
            }

            val win = contents[i]
            win.render(display, col, y)
            col += win.width
        }
    }

    override fun getXPositionOf(window: IJLineWindow): Int {
        var xOffset = 0
        for (row in contents) {
            val rowX = row.getYPositionOf(window)
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

    override fun resize(width: Int, height: Int) {
        val rows = contents.size
        val separators = rows - 1
        var availableWidth = width - separators

        var remainingRows = rows - 1
        for (item in contents.sortedByDescending { it.lastResizeRequest }) {
            val requestedWidth = item.width
            val allottedWidth =
                if (remainingRows == 0) availableWidth
                else maxOf(
                    WINDOW_MIN_WIDTH,
                    minOf(
                        requestedWidth,
                        // leave room for the remaining rows
                        availableWidth - WINDOW_MIN_WIDTH * remainingRows
                    )
                )
            availableWidth -= allottedWidth

            item.resize(allottedWidth, height)
            --remainingRows
        }
    }

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

