package net.dhleong.judo

/**
 * @author dhleong
 */

fun main(args: Array<String>) {
    // prevent OSX from showing the title bar and dock icon
    System.setProperty("apple.awt.UIElement", "true")

    val renderer = JLineRenderer()
    renderer.validate()

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            renderer.close()
        }
    })

    val judo = JudoCore(renderer)

    if (args.size == 2) {
        try {
            val host = args[0]
            val port = args[1].toInt()
            judo.connect(host, port)
        } catch (e: NumberFormatException) {
            System.err.println("Invalid port: ${args[1]}")
            System.exit(1)
        }
    }

    judo.readKeys(renderer)
}
