package net.dhleong.judo.mapping

import net.dhleong.judo.render.IJudoWindow
import java.io.File

/**
 * Public interface of IMapManager; if a MapManager is to be
 * exposed to scripting, only these methods should be exposed
 *
 * @author dhleong
 */
interface IMapManagerPublic {
    companion object {
        private const val DEFAULT_MAP_CAPACITY = 50000
    }

    val current: IJudoMap?
    var window: IJudoWindow?

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
     * Render the current map
     */
    fun render() = render(window ?: throw IllegalStateException("No window to render into"))
    fun render(intoWindow: IJudoWindow)

    /**
     * Called when the attached window may have changed in size
     */
    fun onResize()

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
     * Process the given text like a movement command,
     *  updating the map to follow the exit, creating
     *  the room if appropriate and desired.
     * This could be useful when following someone to
     *  keep the map updated
     */
    @Suppress("unused") fun command(text: String)

    /**
     * Set the current room id
     */
    @Suppress("unused") fun goto(roomId: Int): Boolean {
        val map = current
        if (map != null) {
            map.inRoom = roomId
            return true
        }
        return false
    }
}

/**
 * Full IMapManager interface, with extra members
 *  that probably shouldn't be exposed to scripting,
 *  but which implementations of IJudoCore might need
 */
interface IMapManager : IMapManagerPublic {
    /**
     * Reset the IMapManager state
     */
    fun clear()

    /**
     * Attempt to handle the text as a command; if it appears to
     * be a direction, we will update the map to follow the
     * direction, creating a new room if appropriate and requested
     */
    fun maybeCommand(text: String)
}
