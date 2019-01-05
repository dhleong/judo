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

            BRIGHT_BLACK(8),
            BRIGHT_RED(9),
            BRIGHT_GREEN(10),
            BRIGHT_YELLOW(11),
            BRIGHT_BLUE(12),
            BRIGHT_MAGENTA(13),
            BRIGHT_CYAN(14),
            BRIGHT_WHITE(15),
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

                8 -> Color.BRIGHT_BLACK
                9 -> Color.BRIGHT_RED
                10 -> Color.BRIGHT_GREEN
                11 -> Color.BRIGHT_YELLOW
                12 -> Color.BRIGHT_BLUE
                13 -> Color.BRIGHT_MAGENTA
                14 -> Color.BRIGHT_CYAN
                15 -> Color.BRIGHT_WHITE

                else -> throw IllegalArgumentException()
            })
        }
    }

    data class High256(
        val value: Int
    ) : JudoColor()

    data class FullRGB(
        val red: Int,
        val green: Int,
        val blue: Int
    ) : JudoColor()
}
