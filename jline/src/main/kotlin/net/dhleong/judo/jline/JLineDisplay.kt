package net.dhleong.judo.jline

import org.jline.utils.AttributedStringBuilder

class JLineDisplay(
    val width: Int
) {
    val lines = mutableListOf<AttributedStringBuilder>()

    inline fun withLine(x: Int, y: Int, block: AttributedStringBuilder.() -> Unit) {
        if (y > lines.size) {
            for (i in 0 until y) {
                lines += AttributedStringBuilder(width).apply {
                    for (i in 0 until width) {
                        append(" ")
                    }
                }
            }
        }

        lines[y].let { builder ->
            if (builder.length < x) {
                for (i in builder.length until x) {
                    builder.append(" ")
                }
            } else if (builder.length > x) {
                builder.setLength(x)
            }

            builder.block()
        }
    }

    fun toAttributedStrings() = lines.map { it.toAttributedString() }
}