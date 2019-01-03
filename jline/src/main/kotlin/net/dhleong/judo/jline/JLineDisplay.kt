package net.dhleong.judo.jline

import org.jline.utils.AttributedStringBuilder

class JLineDisplay(
    private var myWidth: Int,
    private var myHeight: Int
) {

    val width: Int
        get() = myWidth
    val height: Int
        get() = myHeight

    val lines: List<AttributedStringBuilder> = ArrayList<AttributedStringBuilder>(myHeight).also {
        for (i in 0 until myHeight) {
            it += AttributedStringBuilder(myWidth).apply {
                for (i in 0 until myWidth) {
                    append(" ")
                }
            }
        }
    }

    fun resize(windowWidth: Int, windowHeight: Int) {
        for (i in myHeight until windowHeight) {
            (lines as MutableList) += AttributedStringBuilder(windowWidth).apply {
                for (i in 0 until windowWidth) {
                    append(" ")
                }
            }
        }

        myWidth = windowWidth
        myHeight = windowHeight
    }

    inline fun withLine(x: Int, y: Int, block: AttributedStringBuilder.() -> Unit) {
        if (y >= width) throw IndexOutOfBoundsException(
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

            builder.block()

            if (builder.length > width) {
                throw IndexOutOfBoundsException("Wrote on line #$y past display width ($width)")
            }

            builder.setLength(width) // restore length
        }
    }

    fun toAttributedStrings() = lines.map { it.toAttributedString() }

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