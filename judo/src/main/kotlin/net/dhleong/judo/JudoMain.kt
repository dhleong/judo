package net.dhleong.judo

/**
 * @author dhleong
 */

fun main(args: Array<String>) {
    // prevent OSX from showing the title bar and dock icon
    System.setProperty("apple.awt.UIElement", "true")

    val renderer = JLineRenderer()

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            renderer.close()
        }
    })

    val judo = JudoCore(renderer)
    judo.readKeys(renderer)
}
