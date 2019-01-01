package net.dhleong.judo.jline

import org.jline.utils.AttributedStringBuilder

class JLineDisplay(
    val width: Int,
    val height: Int
) {
    val lines: List<AttributedStringBuilder> = ArrayList<AttributedStringBuilder>(height).also {
        for (i in 0 until height) {
            it += AttributedStringBuilder(width).apply {
                for (i in 0 until width) {
                    append(" ")
                }
            }
        }
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
        if (width != other.width) return false
        return lines == other.lines
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + lines.hashCode()
        return result
    }
}