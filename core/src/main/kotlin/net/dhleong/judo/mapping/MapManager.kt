package net.dhleong.judo.mapping

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.MAP_AUTOMAGIC
import net.dhleong.judo.MAP_AUTORENDER
import net.dhleong.judo.MAP_AUTOROOM
import net.dhleong.judo.StateMap
import net.dhleong.judo.event.EVENT_GMCP_ENABLED
import net.dhleong.judo.event.EVENT_MSDP_ENABLED
import net.dhleong.judo.inTransaction
import net.dhleong.judo.render.IJudoWindow
import java.io.File

/**
 * @author dhleong
 */
class MapManager(
    private val judo: IJudoCore,
    private val settings: StateMap,
    private val mapRenderer: MapRenderer
) : IMapManager {

    override var current: IJudoMap? = null
    override var window: IJudoWindow? = null

    private var currentFormat: String? = null
    private var currentFile: File? = null

    private val formats = arrayOf(TintinMapFormat())
        .fold(mutableMapOf<String, MapFormat>()) { map, format ->
            map[format.name] = format
            map
        }

    private var magicMapper: AutomagicMapper? = null

    override fun clear() {
        current = null
        currentFormat = null
        currentFile = null
        magicMapper?.let {
            it.clear()
            magicMapper = null
        }
    }

    override fun createEmpty(capacity: Int) {
        // NOTE: JudoMap doesn't currently use an explicit capacity
        current = JudoMap()
        init()
    }

    override fun load(file: File) {
        if (!file.exists()) throw IllegalArgumentException("No such file $file")

        val map = JudoMap()
        val format = "tintin" // the only one we support right now
        currentFile = file
        currentFormat = format

        file.inputStream().use {
            formats[format]!!.read(map, it)
        }

        current = map
        init()
    }

    override fun render(intoWindow: IJudoWindow) {
        current?.let {
            mapRenderer.resize(intoWindow.width, intoWindow.visibleHeight)

            judo.renderer.inTransaction {
                mapRenderer.renderMap(it, intoWindow)
            }
        }
    }

    override fun onResize() {
        val w = window ?: return
        mapRenderer.resize(w.width, w.visibleHeight)
    }

    override fun save() {
        val file = currentFile
        val format = currentFormat
        if (file == null) throw IllegalStateException("No map file loaded")
        if (format == null) throw IllegalStateException("Unknown map format")
        saveAs(file, format)
    }

    override fun saveAs(file: File, format: String) {
        val map = current ?: throw IllegalStateException("No map to save")

        currentFile = file
        currentFormat = format

        file.outputStream().use {
            formats[format]!!.write(map, it)
        }
    }

    override fun command(text: String) {
        val map = current ?: return
        val room = map.currentRoom ?: return

        val exit = room.exits[text]
        if (exit != null) {
            map.inRoom = exit.room.id
        } else if (settings[MAP_AUTOROOM]) {
            val newRoom = JudoRoom(map.lastRoom + 1, text)
            map.dig(room, text, newRoom)
            map.inRoom = newRoom.id
        }

        magicMapper?.let {
            if (text in map.reverseCmds) {
                it.onMove(text)
            }
        }

        if (settings[MAP_AUTORENDER]) {
            render()
        }
    }

    override fun maybeCommand(text: String) {
        // TODO support user-provided reverseCmds
        val map = current ?: return
        if (text in map.reverseCmds) {
            command(text)
        }
    }

    private fun init() {

        // use the current window by default
        window = judo.renderer.currentTabpage.currentWindow

        if (!settings[MAP_AUTOMAGIC]) return

        val mapper = AutomagicMapper(judo, this)
        magicMapper = mapper

        when {
            judo.connection?.isGmcpEnabled == true -> mapper.onGmcpAvailable()

            judo.connection?.isMsdpEnabled == true -> mapper.onMsdpAvailable()

            else -> {
                judo.events.register(EVENT_GMCP_ENABLED) { mapper.onGmcpAvailable() }
                judo.events.register(EVENT_MSDP_ENABLED) { mapper.onMsdpAvailable() }
            }
        }
    }
}

