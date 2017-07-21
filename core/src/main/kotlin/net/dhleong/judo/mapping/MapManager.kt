package net.dhleong.judo.mapping

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.MAP_AUTOROOM
import net.dhleong.judo.StateMap
import java.io.File

/**
 * @author dhleong
 */
class MapManager(
    private val judo: IJudoCore,
    private val settings: StateMap
) : IMapManager {

    override var current: IJudoMap? = null

    private var currentFormat: String? = null
    private var currentFile: File? = null

    private val formats = arrayOf(TintinMapFormat())
        .fold(mutableMapOf<String, MapFormat>()) { map, format ->
            map[format.name] = format
            map
        }

    override fun createEmpty(capacity: Int) {
        // NOTE: JudoMap doesn't currently use an explicit capacity
        current = JudoMap()
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

        room.exits[text]?.let {
            map.inRoom = it.room.id
            return
        }

        if (settings[MAP_AUTOROOM]) {
            val newRoom = JudoRoom(map.lastRoom + 1, text)
            map.dig(room, text, newRoom)
            map.inRoom = newRoom.id
        }
    }

    override fun maybeCommand(text: String) {
        // TODO support user-provided reverseCmds
        if (text in JudoMap.reverseCmds) {
             command(text)
        }
    }
}