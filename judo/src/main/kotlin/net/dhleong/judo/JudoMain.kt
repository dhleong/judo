package net.dhleong.judo

import net.dhleong.judo.input.toChannelFactory
import net.dhleong.judo.jline.JLineRenderer
import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.mapping.renderer.DelegateMapRenderer
import net.dhleong.judo.mapping.renderer.SimpleBufferMapRenderer
import net.dhleong.judo.net.CompositeConnectionFactory
import net.dhleong.judo.net.JudoConnection
import net.dhleong.judo.net.TelnetConnection
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

    val argsList = args.toMutableList()
    val hasDebug = argsList.remove("--debug")
    val hasNetDebug = argsList.remove("--debug=net")
    val hasAllDebug = argsList.remove("--debug=all")
    val debugLevel = when {
        hasAllDebug -> DebugLevel.ALL
        hasDebug -> DebugLevel.NORMAL
        else -> DebugLevel.OFF
    }

    val connections: JudoConnection.Factory = CompositeConnectionFactory(listOf(
        // normal telnet
        TelnetConnection.Factory(
            debug = debugLevel.isEnabled,
            logRaw = hasNetDebug
        ),

        // secure telnet
        TelnetConnection.SecureFactory(
            debug = debugLevel.isEnabled,
            logRaw = hasNetDebug
        )
    ))

    val init = try {
        InitStrategy.pick(argsList)
    } catch (e: Exception) {
        // unable to pick a strategy
        println(e.message)
        System.exit(1)
        return
    }

    // make sure we can render
    val ids = IdManager()
    val renderer = JLineRenderer(ids, settings)
    renderer.validate()

    val mapRenderer: MapRenderer = DelegateMapRenderer(
        SimpleBufferMapRenderer()
    )

    // clean up after ourselves
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            if (!closed) {
                renderer.close()
            }
        }
    })

    // the main thing
    val judo = JudoCore(
        renderer,
        mapRenderer,
        settings,
        connections = connections,
        userConfigDir = USER_CONFIG_DIR,
        userConfigFile = init.config.userConfigFile,
        scripting = init.config.engineFactory,
        debug = debugLevel
    )

    // connect or read a world file based on the init strategy
    if (!init.perform(judo)) {
        closed = true
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
    judo.readKeys(renderer.toChannelFactory())
}
