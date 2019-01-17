package net.dhleong.judo.jline

import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

class JLineDisplay(
    private var myWidth: Int,
    private var myHeight: Int
) {

    val width: Int
        get() = myWidth
    val height: Int
        get() = myHeight

    var cursorRow: Int = 0
    var cursorCol: Int = 0

    val lines: List<AttributedStringBuilder> = ArrayList<AttributedStringBuilder>(myHeight).also {
        for (i in 0 until myHeight) {
            it += newBuilder(myWidth).apply {
                for (j in 0 until myWidth) {
                    append(" ")
                }
            }
        }
    }

    /**
     * "scroll" all the lines of this display by [amount], using the same
     * directional semantics as [IJLineWindow.scrollLines]. The location
     * of the lines shifted "off screen" and the contents of the lines
     * "newly on screen" is undefined—only the lines that "remained on
     * screen" can be relied upon
     *
     * @param [amount] currently must be positive
     */
    fun scroll(amount: Int) {
        if (amount < 0) throw UnsupportedOperationException()

        lines as MutableList<AttributedStringBuilder>
        val len = myHeight
        val remaining = len - amount
        for (i in 0 until remaining) {
            val source = i + amount
            if (source < 0 || source > len) break

            // swap so we're not sharing instances anywhere
            val old = lines[i]
            lines[i] = lines[source]
            lines[source] = old
        }
    }

    fun resize(windowWidth: Int, windowHeight: Int) {
        for (i in myHeight until windowHeight) {
            (lines as MutableList) += newBuilder(windowWidth).apply {
                for (j in 0 until windowWidth) {
                    append(" ")
                }
            }
        }

        myWidth = windowWidth
        myHeight = windowHeight
    }

    fun clearLine(x: Int, y: Int, fromRelativeX: Int, toRelativeX: Int) {
        if (x + fromRelativeX == toRelativeX) {
            // nop
            return
        }

        withLine(x + fromRelativeX, y) {
            for (j in 0 until (toRelativeX - fromRelativeX)) {
                append(" ")
            }
        }
    }

    /**
     * @param [lineWidth] if provided, the width of the line to be edited.
     *  Any amount not filled by the [block] will be cleared
     */
    inline fun withLine(
        x: Int, y: Int,
        lineWidth: Int = -1,
        block: AttributedStringBuilder.() -> Unit
    ) {
        if (x >= width) throw IndexOutOfBoundsException(
            "Attempting to update non-existent column; windowWidth=$width; x=$x"
        )
        if (y >= height) throw IndexOutOfBoundsException(
            "Attempting to update non-existent line; windowHeight=$height; y=$y"
        )

        lines[y].let { builder ->
            if (builder.length < x) {
                for (i in builder.length until x) {
                    builder.append(" ")
                }
            } else if (builder.length > x) {
                builder.setLength(x)
            }

            val lengthBefore = builder.length
            builder.block()
            val lengthAfter = builder.length

            if (builder.length > width) {
                throw IndexOutOfBoundsException("""
                    Wrote on line #$y past display width $width; lineWidth=$lineWidth; line:
                    `$builder`
                """.trimIndent())
            }

            if (lineWidth > 0) {
                // JLine doesn't automatically set the "current" style,
                // so... do it for them
                if (lengthAfter > lengthBefore) {
                    builder.style(
                        builder.styleAt(builder.lastIndex)
                    )
                }

                clearLine(
                    x, y,
                    fromRelativeX = lengthAfter - lengthBefore,
                    toRelativeX = lineWidth
                )

                if (lengthAfter > lengthBefore) {
                    builder.style(AttributedStyle.DEFAULT)
                }
            }

            builder.setLength(width) // restore length
        }
    }

    fun toAttributedStrings() =
        lines.take(myHeight)
            .map { it.toAttributedString() }

    private fun newBuilder(windowWidth: Int) = AttributedStringBuilder(windowWidth).apply {
        tabs(2)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is JLineDisplay) return false
        if (myWidth != other.myWidth) return false
        if (myHeight != other.myHeight) return false
        return lines.take(myHeight) == other.lines.take(myHeight)
    }

    override fun hashCode(): Int {
        var result = myWidth
        result = 31 * result + lines.hashCode()
        return result
    }
}