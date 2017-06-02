package net.dhleong.judo

import java.io.File

/**
 * @author dhleong
 */

val USER_HOME = System.getProperty("user.home")!!
val userConfigFile = File("$USER_HOME/.config/judo/init.py").absoluteFile!!

fun main(args: Array<String>) {
    // prevent OSX from showing the title bar and dock icon
    System.setProperty("apple.awt.UIElement", "true")

    // make sure we can render
    val renderer = JLineRenderer()
    renderer.validate()

    // clean up after ourselves
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            renderer.close()
        }
    })

    // the main thing
    val judo = JudoCore(renderer)

    // if they have a global init.py, read it
    if (userConfigFile.exists()) {
        judo.readFile(userConfigFile)
    }

    when (args.size) {
        1 -> {
            // read world.py
            val worldPy = File(args[0].replace("^~", USER_HOME)).absoluteFile
            if (!(worldPy.exists() && worldPy.canRead())) {
                judo.quit()
                renderer.close()
                System.err.println("Unable to open world file $worldPy")
                System.exit(2)
                return
            }

            judo.readFile(worldPy)
        }

        2 -> {
            // connect directly
            try {
                val host = args[0]
                val port = args[1].toInt()
                judo.connect(host, port)
            } catch (e: NumberFormatException) {
                System.err.println("Invalid port: ${args[1]}")
                System.exit(1)
            }
        }
    }

    // finally, start handling user input
    judo.readKeys(renderer)
}
