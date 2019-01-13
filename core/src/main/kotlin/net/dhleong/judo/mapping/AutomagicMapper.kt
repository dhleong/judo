package net.dhleong.judo.mapping

import net.dhleong.judo.DEBUG_AUTOMAGIC
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.MAP_AUTORENDER
import net.dhleong.judo.event.EventHandler
import net.dhleong.judo.net.MSDP_VAL
import net.dhleong.judo.net.MSDP_VAR
import net.dhleong.judo.net.TELNET_IAC
import net.dhleong.judo.net.TELNET_SB
import net.dhleong.judo.net.TELNET_SE
import net.dhleong.judo.net.TELNET_TELOPT_GMCP
import net.dhleong.judo.net.TELNET_TELOPT_MSDP
import net.dhleong.judo.util.Json

/**
 * @author dhleong
 */
class AutomagicMapper(
    internal val judo: IJudoCore,
    internal val mapper: MapManager
) {
    enum class State {
        NONE,
        GMCP,
        MSDP_FETCH,
        MSDP
    }

    private var state = State.NONE

    private var lastVnum: String? = null
    private var lastExits: Map<String, Any>? = null
    private var lastName: String? = null

    private val msdpWorkspace = StringBuilder(256)
    private val registered = HashMap<String, EventHandler>()
    private var reportsName = false
    private var mapStrategy: MapStrategy? = null

    fun clear() {
        registered.forEach { k, v ->
            judo.events.unregister(k, v)
        }
        registered.clear()
    }

    /**
     * We received a command to move in a direction
     */
    fun onMove(cmd: String) {
        mapStrategy?.onMove(cmd)
    }

    fun onGmcpAvailable() {
        if (state == State.MSDP || state == State.MSDP_FETCH) {
            unregisterEvent("MSDP:REPORT_VARIABLES")

            if (state == State.MSDP) {
                sendMsdp("UNREPORT", "ROOM")
            }
        }

        registerEvent("GMCP:room.info", this::onGmcpRoom)
        sendGmcp("Core.Supports.Add", arrayOf("Room 1", "Room.Info 1"))
    }

    fun onMsdpAvailable() {
        if (state == State.GMCP) {
            // if we have gmcp, we probably don't need msdp
            return
        }

        registerEvent("MSDP:REPORTABLE_VARIABLES", this::onVarsList)
        sendMsdp("LIST", "REPORTABLE_VARIABLES")
    }

    fun onVarsList(vars: List<String>) {
        unregisterEvent("MSDP:REPORTABLE_VARIABLES")

        if ("ROOM" in vars) {
            registerEvent("MSDP:ROOM", this::onRoom)
            sendMsdp("REPORT", "ROOM")
        } else if ("ROOMVNUM" in vars && "ROOMEXITS" in vars) {
            registerEvent("MSDP:ROOMVNUM", this::onRoomVnum)
            registerEvent("MSDP:ROOMEXITS", this::onRoomExits)
            sendMsdp("REPORT", "ROOMVNUM")
            sendMsdp("REPORT", "ROOMEXITS")

            if ("ROOMNAME" in vars) {
                registerEvent("MSDP:ROOMNAME", this::onRoomName)
                sendMsdp("REPORT", "ROOMNAME")
                reportsName = true
            }
        } else {
            state = State.NONE
            judo.printRaw("No way to automap using $vars")
        }
    }

    /*
     Room-based automapping
     */

    @Suppress("UNCHECKED_CAST")
    fun onGmcpRoom(room: Map<String, Any>) {
        if ("num" !in room || "exits" !in room) {
            state = State.NONE
            judo.printRaw("No way to automap using $room")
            sendGmcp("Core.Supports.Remove",
                arrayOf("Room", "Room.Info"))
            return
        }

        val vnum = room["num"] as Int? ?: return
        val exits = room["exits"] as Map<String, Any>? ?: return
        val name = room["name"] as String? ?: return

        onRoom(vnum, name, exits)
    }

    @Suppress("UNCHECKED_CAST")
    fun onRoom(room: Map<String, Any>) {
        val vnum = room["vnum"] as String? ?: return
        val exits = room["exits"] as Map<String, Any>? ?: return
        val name = room["name"] as String?
            ?: if (reportsName) return
               else lastVnum!!

        onRoom(vnum.toInt(), name, exits)
    }

    fun onRoom(vnum: Int, name: String, exits: Map<String, Any>) {
        if (judo.state[DEBUG_AUTOMAGIC]) {
            judo.printRaw("onRoom($vnum, $name) $exits")
        }

        val strategy = mapStrategy
            ?: createStrategy(exits).also { mapStrategy = it }

        strategy.onRoom(vnum, name, exits)

        if (judo.state[MAP_AUTORENDER]) {
            mapper.render()
        }
    }

    /*
     vnum + exits
     */

    fun onRoomVnum(vnum: String) {
        lastVnum = vnum
        checkRoom()
    }

    fun onRoomExits(exits: Map<String, Any>?) {
        lastExits = exits
        checkRoom()
    }

    fun onRoomName(name: String?) {
        lastName = name
        checkRoom()
    }


    private fun checkRoom() {
        val exits = lastExits ?: return
        val name = lastName
            ?: if (reportsName) return
               else lastVnum!!
        val vnum = lastVnum?.toInt() ?: return

        onRoom(vnum, name, exits)

        lastVnum = null
        lastExits = null
        lastName = null
    }

    private fun createStrategy(exits: Map<String, Any>): MapStrategy {
        return if (exits.any { (it.value as String).toIntOrNull() != null }) {
            // if the exit maps to an int vnum, that means we can
            // use the "easy" map strategy
            EasyMapStrategy(this)
        } else {
            // if not an int, we don't know the exit target vnums,
            // so we have to intuit the links
            DirectionalMapStrategy(this)
        }
    }


    private fun <T> registerEvent(event: String, handler: (T) -> Unit) {
        val realHandler = coerce(handler)
        judo.events.register(event, realHandler)
        registered[event] = realHandler
    }

    private fun unregisterEvent(event: String) {
        val oldHandler = registered.remove(event)
        oldHandler?.let {
            judo.events.unregister(event, oldHandler)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <T> coerce(crossinline fn: (T) -> Unit): EventHandler = { arg ->
        fn(arg as T)
    }

    private fun sendGmcp(packageName: String, value: Any? = null) =
        judo.send(buildGmcp(packageName, value), true)

    private fun buildGmcp(key: String, value: Any?): String {
        return with(msdpWorkspace) {
            setLength(0)

            append(TELNET_IAC.toChar())
            append(TELNET_SB.toChar())

            append(TELNET_TELOPT_GMCP.toChar())
            append(key)

            if (value != null) {
                append(' ')
                append(Json.write(value))
            }

            append(TELNET_IAC.toChar())
            append(TELNET_SE.toChar())
        }.toString()
    }

    private fun sendMsdp(key: String, value: String) =
        judo.send(buildMsdp(key, value), true)

    private fun buildMsdp(key: String, value: String): String {
        return with(msdpWorkspace) {
            setLength(0)

            append(TELNET_IAC.toChar())
            append(TELNET_SB.toChar())

            append(TELNET_TELOPT_MSDP.toChar())
            append(MSDP_VAR.toChar())
            append(key)
            append(MSDP_VAL.toChar())
            append(value)

            append(TELNET_IAC.toChar())
            append(TELNET_SE.toChar())
        }.toString()
    }
}

interface MapStrategy {
    fun onRoom(vnum: Int, name: String, exits: Map<String, Any>)
    fun onMove(cmd: String) {
        // ignore by default
    }
}

internal class EasyMapStrategy(private val mapper: AutomagicMapper) : MapStrategy {
    override fun onRoom(vnum: Int, name: String, exits: Map<String, Any>) {
        val map = mapper.mapper.current!!
        if (vnum !in map) {
            val actualExits = JudoExits()
            exits.forEach { exit, exitVnumStr ->
                val exitVnum = (exitVnumStr as String).toInt()
                if (exitVnum in map) {
                    actualExits[exit] = map[exitVnum]!!
                } else {
                    val newRoom = JudoRoom(exitVnum, exitVnumStr)
                    map.add(newRoom)
                    actualExits[exit] = newRoom
                }
            }

            map += JudoRoom(vnum, name, actualExits)
        }

        map.inRoom = vnum
        map.currentRoom!!.name = name
    }
}

internal class DirectionalMapStrategy(private val mapper: AutomagicMapper) : MapStrategy {

    var pendingMoves = mutableListOf<String>()

    override fun onRoom(vnum: Int, name: String, exits: Map<String, Any>) {
        // NOTE: this is a bit trickier since we aren't provided the vnums
        // for the exits, so we have to intuit the links
        val map = mapper.mapper.current!!
        val room = map.currentRoom

        if (pendingMoves.isEmpty() || pendingMoves.size > 1 || room == null) {
            // easy case: no moves yet; either initial room, or some sort of teleport
            // just go to there
            if (vnum !in map) {
                map += JudoRoom(vnum, name)
            }

            if (pendingMoves.size > 1) {
                // we had multiple move commands, so don't bother trying
                // to create exits; we might have hit a wall, for example
            }
        } else if (room.id != vnum) {
            // normal case. one move, one room
            val pendingMove = normalizeMoveCmd(pendingMoves[0])
            val newRoom = map[vnum] ?: JudoRoom(vnum, name).also {
                map += it
            }
            room.exits[pendingMove] = newRoom

            val reverse = map.reverseCmds[pendingMove]
            if (reverse != null && reverse in exits.keys.map { normalizeMoveCmd(it) }) {
                newRoom.exits[reverse] = room
            }
        }

        map.inRoom = vnum
        pendingMoves.clear()
    }

    override fun onMove(cmd: String) {
        val map = mapper.mapper.current!!
        map.currentRoom?.let { room ->
            val dir = normalizeMoveCmd(cmd)
            room.exits[dir]?.let {
                // just follow a known link
                map.inRoom = it.room.id
                return
            }
        }

        pendingMoves.add(cmd)
    }
}
