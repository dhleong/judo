package net.dhleong.judo.mapping

/**
 * @author dhleong
 */

interface IJudoMap : Iterable<IJudoRoom> {
    /**
     * Map of a direction command to its reverse
     */
    val reverseCmds: Map<String, String>

    val size: Int

    var inRoom: Int?
    val lastRoom: Int

    val currentRoom: IJudoRoom?
        get() = inRoom?.let { this[it] }

    operator fun contains(roomId: Int): Boolean
    operator fun get(roomId: Int): IJudoRoom?
    operator fun plusAssign(room: IJudoRoom) = add(room)
    fun add(room: IJudoRoom)
    fun deleteRoom(roomId: Int)
    fun dig(from: IJudoRoom, cmd: String, newRoom: IJudoRoom)
}

interface IJudoRoom {
    val id: Int
    var name: String
    val exits: IJudoExits

    /** Used for grid generation */
    var flags: Int
}

interface IJudoExits : Iterable<IJudoExit> {
    val size: Int

    operator fun contains(exitCmd: String): Boolean
    operator fun get(cmd: String): IJudoExit?
    operator fun set(cmd: String, exit: IJudoExit)
    operator fun set(cmd: String, exitRoom: IJudoRoom)

    /** Delete the exit with the given cmd, if we have it */
    fun remove(cmd: String): IJudoExit?

    /** Delete the exit to the given roomId, if we have one */
    fun removeExitTo(roomId: Int): IJudoExit?
}

interface IJudoExit {
    companion object {
        val DIR_N = 1
        val DIR_E = 2
        val DIR_S = 4
        val DIR_W = 8
        val DIR_U = 16
        val DIR_D = 32
    }

    val room: IJudoRoom
    val name: String
    val cmd: String
    val dirs: Int
    val flags: Int
}