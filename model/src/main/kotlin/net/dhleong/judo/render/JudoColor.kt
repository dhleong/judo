package net.dhleong.judo.render

sealed class JudoColor {
    operator fun plus(other: JudoColor): JudoColor {
        if (other == Default) return this
        return other
    }

    object Default : JudoColor()

    data class Simple(
        val value: Color
    ) : JudoColor() {
        enum class Color(
            val ansi: Int
        ) {
            BLACK(0),
            RED(1),
            GREEN(2),
            YELLOW(3),
            BLUE(4),
            MAGENTA(5),
            CYAN(6),
            WHITE(7),

            // TODO bright colors
        }

        companion object {
            fun from(value: Int) = JudoColor.Simple(when (value) {
                0 -> Color.BLACK
                1 -> Color.RED
                2 -> Color.GREEN
                3 -> Color.YELLOW
                4 -> Color.BLUE
                5 -> Color.MAGENTA
                6 -> Color.CYAN
                7 -> Color.WHITE

                else -> throw IllegalArgumentException()
            })
        }
    }

    data class High256(
        val value: Int
    ) : JudoColor() // TODO

    data class FullRGB(
        val red: Int,
        val green: Int,
        val blue: Int
    ) : JudoColor()
}
