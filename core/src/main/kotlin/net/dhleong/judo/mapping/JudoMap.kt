package net.dhleong.judo.mapping

/**
 * @author dhleong
 */
class JudoMap : IJudoMap {
    companion object {
        val reverseCmds = mutableMapOf(
            "n" to "s",
            "s" to "n",
            "w" to "e",
            "e" to "w",
            "nw" to "se",
            "se" to "nw",
            "ne" to "sw",
            "sw" to "ne",
            "u" to "d",
            "d" to "u"
        )

    }

    private val rooms = HashMap<Int, IJudoRoom>()

    override val size: Int
        get() = rooms.size

    override var inRoom: Int? = null
    override var lastRoom: Int = 0

    override fun contains(roomId: Int): Boolean = roomId in rooms
    override fun get(roomId: Int): IJudoRoom? = rooms[roomId]

    override fun add(room: IJudoRoom) {
        rooms[room.id] = room
        lastRoom = room.id
    }

    override fun deleteRoom(roomId: Int) {
        rooms.remove(roomId)
        this.forEach {
            it.exits.removeExitTo(roomId)
        }
    }

    override fun iterator(): Iterator<IJudoRoom> = rooms.values.iterator()

    override fun dig(from: IJudoRoom, cmd: String, newRoom: IJudoRoom) {
        add(newRoom)
        from.exits[cmd] = JudoExit(newRoom, cmd, cmd)
        reverseCmds[cmd]?.let {
            newRoom.exits[it] = JudoExit(from, it, it)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as JudoMap

        if (rooms != other.rooms) return false
        if (inRoom != other.inRoom) return false
        if (lastRoom != other.lastRoom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rooms.hashCode()
        result = 31 * result + (inRoom ?: 0)
        result = 31 * result + lastRoom
        return result
    }
}

data class JudoRoom(
    override val id: Int,
    override var name: String,
    override val exits: JudoExits = JudoExits(),
    override var flags: Int = 0
) : IJudoRoom

class JudoExits : IJudoExits {
    private val exits = HashMap<String, IJudoExit>()

    override fun contains(exitCmd: String): Boolean = exitCmd in exits
    override fun get(cmd: String): IJudoExit? = exits[cmd.toLowerCase()]
    override fun remove(cmd: String): IJudoExit? = exits.remove(cmd)
    override fun removeExitTo(roomId: Int): IJudoExit? {
        val exitsIter = exits.values.iterator()
        for (exit in exitsIter) {
            if (exit.room.id == roomId) {
                exitsIter.remove()
                return exit
            }
        }
        return null
    }
    override fun set(cmd: String, exit: IJudoExit) {
        exits[cmd.toLowerCase()] = exit
    }
    operator fun set(exitCmd: String, room: IJudoRoom) {
        this[exitCmd] = JudoExit(room, exitCmd, exitCmd)
    }

    override fun iterator(): Iterator<IJudoExit> = exits.values.iterator()
}

data class JudoExit(
    override val room: IJudoRoom,
    override val name: String,
    override val cmd: String,
    override val flags: Int = 0
) : IJudoExit {
    override val dirs: Int = extractDirs(cmd)

    private fun extractDirs(cmd: String): Int {
        var dirs = 0
        when {
            (cmd == "u") -> dirs = IJudoExit.DIR_U
            (cmd == "d") -> dirs = IJudoExit.DIR_D
            (cmd.length <= 2) -> {
                if ("n" in cmd) dirs += IJudoExit.DIR_N
                if ("s" in cmd) dirs += IJudoExit.DIR_S
                if ("e" in cmd) dirs += IJudoExit.DIR_E
                if ("w" in cmd) dirs += IJudoExit.DIR_W
            }
        }
        return dirs
    }
}
