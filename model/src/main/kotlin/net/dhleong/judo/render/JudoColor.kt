package net.dhleong.judo.render

sealed class JudoColor {
    operator fun plus(other: JudoColor): JudoColor {
        if (other == Default) return this
        return other
    }

    object Default : JudoColor()

    class Simple(
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
    }

    class High256(
        val value: Int
    ) : JudoColor() // TODO

    class FullRGB(
        val red: Int,
        val green: Int,
        val blue: Int
    ) : JudoColor()
}
