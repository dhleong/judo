package net.dhleong.judo.render

sealed class JudoColor {
    class Simple(
        val value: Color
    ) : JudoColor() {
        enum class Color {
            BLACK,
            RED,
            GREEN,
            YELLOW,
            BLUE,
            MAGENTA,
            CYAN,
            WHITE,
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
