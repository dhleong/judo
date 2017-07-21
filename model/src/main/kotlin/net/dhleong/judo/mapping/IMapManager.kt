package net.dhleong.judo.mapping

import java.io.File

/**
 * @author dhleong
 */
interface IMapManager {
    companion object {
        private val DEFAULT_MAP_CAPACITY = 50000
    }

    val current: IJudoMap?

    /**
     * Create a new, empty Map with at least the given capacity
     */
    fun createEmpty(capacity: Int = DEFAULT_MAP_CAPACITY)

    // scripting overloads
    @Suppress("unused") fun createEmpty() = createEmpty(DEFAULT_MAP_CAPACITY)

    /**
     * Delete the room in the direction [exitCmd]
     * from the current room
     */
    @Suppress("unused") fun deleteRoom(exitCmd: String) {
        val map = current!!
        val inRoom = map[map.inRoom ?: map.lastRoom]!!
        val exit = inRoom.exits.remove(exitCmd)
        val deadRoom = exit?.room
        if (deadRoom != null) {
            deleteRoom(deadRoom.id)
        }
    }

    /**
     * Delete the room with the given id
     */
    fun deleteRoom(roomId: Int) = current!!.deleteRoom(roomId)

    /**
     * Load the current map from disk
     */
    fun load(file: File)

    // scripting overloads
    @Suppress("unused") fun load(file: String) = load(File(file))

    /**
     * Write the current map to disk, overwriting the
     * one we previously loaded in
     */
    fun save()

    /**
     * Write the current map to disk
     */
    fun saveAs(file: File, format: String = "tintin")

    // scripting overloads
    @Suppress("unused") fun saveAs(file: String) = saveAs(File(file))
    @Suppress("unused") fun saveAs(file: String, format: String) = saveAs(File(file), format)


    /**
     * Set the current room id
     */
    @Suppress("unused")
    fun goto(roomId: Int): Boolean {
        val map = current
        if (map != null) {
            map.inRoom = roomId
            return true
        }
        return false
    }
}