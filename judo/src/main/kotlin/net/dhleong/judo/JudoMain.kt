package net.dhleong.judo

import net.dhleong.judo.jline.JLineRenderer
import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.mapping.renderer.DelegateMapRenderer
import net.dhleong.judo.mapping.renderer.SimpleBufferMapRenderer
import net.dhleong.judo.net.CommonsNetConnection
import net.dhleong.judo.net.JudoConnection
import net.dhleong.judo.render.IdManager
import java.io.File

/**
 * @author dhleong
 */

val USER_HOME: String = System.getProperty("user.home")
val USER_CONFIG_DIR: File = File(
    File(USER_HOME, ".config"),
    "judo"
).absoluteFile

fun main(args: Array<String>) {
    var closed = false

    // prevent OSX from showing the title bar and dock icon
    System.setProperty("apple.awt.UIElement", "true")

    if ("--version" in args || "-v" in args) {
        println("judo version ${JudoCore.CLIENT_VERSION}")
        return
    }

    // shared settings
    val settings = StateMap()

    // make sure we can render
//    val renderer = JLineRenderer(settings)
    val ids = IdManager()
    val renderer = JLineRenderer(ids, settings)
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
    val hasNetDebug = argsList.remove("--debug=net")
    val hasAllDebug = argsList.remove("--debug=all")
    val debugLevel = when {
        hasAllDebug -> DebugLevel.ALL
        hasDebug -> DebugLevel.NORMAL
        else -> DebugLevel.OFF
    }

    val mapRenderer: MapRenderer = DelegateMapRenderer(
        SimpleBufferMapRenderer(renderer)
    )

    val connections: JudoConnection.Factory = CommonsNetConnection.Factory(
        debug = debugLevel.isEnabled,
        logRaw = hasNetDebug
    )

    val worldScriptFile = if (argsList.size == 1) {
        File(
            argsList[0].replace("^~", USER_HOME)
        ).absoluteFile
    } else null

    // figure out what our scripting config should be
    val config = ScriptingConfig.pick(USER_CONFIG_DIR, worldScriptFile)

    // the main thing
    val judo = JudoCore(
        renderer,
        mapRenderer,
        settings,
        connections = connections,
        userConfigDir = USER_CONFIG_DIR,
        userConfigFile = config.userConfigFile,
        scripting = config.engineFactory,
        debug = debugLevel
    )

    // if they have a global init.py, read it
    if (config.userConfigFile.exists()) {
        judo.readFile(config.userConfigFile)
    }

    when (argsList.size) {
        1 -> {
            // read world.(script)
            if (worldScriptFile == null) throw IllegalStateException()
            if (!(worldScriptFile.exists() && worldScriptFile.canRead())) {
                closed = true

                judo.quit()
                renderer.close()

                System.err.println("Unable to open world file $worldScriptFile")
                System.exit(2)
                return
            }

            judo.readFile(worldScriptFile)
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

    // we should be good to go; hide any splash screen we may have been showing
    renderer.setLoading(false)

    // finally, start handling user input
    judo.readKeys(renderer)
}

