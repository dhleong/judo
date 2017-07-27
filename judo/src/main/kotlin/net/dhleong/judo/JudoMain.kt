package net.dhleong.judo

import net.dhleong.judo.modes.USER_CONFIG_FILE
import net.dhleong.judo.modes.USER_HOME
import java.io.File

/**
 * @author dhleong
 */

fun main(args: Array<String>) {
    var closed = false

    // prevent OSX from showing the title bar and dock icon
    System.setProperty("apple.awt.UIElement", "true")

    // shared settings
    val settings = StateMap()

    // make sure we can render
    val renderer = JLineRenderer(settings)
    renderer.validate()

    // clean up after ourselves
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            if (!closed) {
                renderer.close()
            }
        }
    })

    val argsList = args.toMutableList()
    val hasDebug = argsList.remove("--debug")
    val hasAllDebug = argsList.remove("--debug=all")
    val debugLevel = when {
        hasAllDebug -> DebugLevel.ALL
        hasDebug -> DebugLevel.NORMAL
        else -> DebugLevel.OFF
    }

    // the main thing
    val judo = JudoCore(renderer, settings, debug = debugLevel)

    // if they have a global init.py, read it
    if (USER_CONFIG_FILE.exists()) {
        judo.readFile(USER_CONFIG_FILE)
    }

    when (argsList.size) {
        1 -> {
            // read world.py
            val worldPy = File(argsList[0].replace("^~", USER_HOME)).absoluteFile
            if (!(worldPy.exists() && worldPy.canRead())) {
                closed = true

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
                val host = argsList[0]
                val port = argsList[1].toInt()
                judo.connect(host, port)
            } catch (e: NumberFormatException) {
                System.err.println("Invalid port: ${argsList[1]}")
                System.exit(1)
            }
        }
    }

    // if they want to use the clipboard as the unnamed register, warm it up;
    //  without this, the first time accessing the clipboard is super slow.
    // We do this now since (at least on macOS) it causes a bit of a visual flash;
    //  doing it while the world is connecting makes it a bit less noticeable. Also,
    //  doing it *before* the world connect stuff starts scrolling in means that lag
    //  to warm up the clipboard delays the initial connection, making everything
    //  feel slower; we're already slow enough thanks to having to warm up Jython
    if ("unnamed" in settings[CLIPBOARD]) {
        judo.registers['*'].value
    }

    // finally, start handling user input
    judo.readKeys(renderer)
}
