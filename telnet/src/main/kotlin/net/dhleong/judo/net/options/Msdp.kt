package net.dhleong.judo.net.options

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.event.EVENT_MSDP_ENABLED
import net.dhleong.judo.net.MSDP_ARRAY_CLOSE
import net.dhleong.judo.net.MSDP_ARRAY_OPEN
import net.dhleong.judo.net.MSDP_TABLE_CLOSE
import net.dhleong.judo.net.MSDP_TABLE_OPEN
import net.dhleong.judo.net.MSDP_VAL
import net.dhleong.judo.net.MSDP_VAR
import net.dhleong.judo.net.TELNET_TELOPT_MSDP
import net.dhleong.judo.net.TelnetClient
import net.dhleong.judo.net.TelnetEvent
import net.dhleong.judo.net.TelnetOptionHandler
import net.dhleong.judo.net.write
import java.io.ByteArrayOutputStream


private const val INITIAL_LIST = "COMMANDS"
private val MSDP_LIST_COMMANDS = buildMsdpRequest("LIST", INITIAL_LIST)

class MsdpHandler(
    private val judo: IJudoCore,
    private val isDebug: () -> Boolean,
    private val printDebug: (String) -> Unit
) : TelnetOptionHandler(
    TELNET_TELOPT_MSDP,
    acceptRemoteWill = true
) {

    val isMsdpEnabled: Boolean
        get() = commands != null

    private val varReader = MsdpReader()
    private var commands: List<String>? = null

    override fun onSubnegotiation(client: TelnetClient, event: TelnetEvent) {
        // NOTE event[0] == SB *always*
        // NOTE event[1] == MSDP *always*
        if (event[2] == MSDP_VAR) {

            varReader.reset(event, 3)
            val name = varReader.readString()
            val value = varReader.readObject()

            if (isDebug()) {
                printDebug("## TELNET < MSDP VAR: $name <- $value")
            }

            var gotCommands = false
            if (name == "COMMANDS") {
                gotCommands = commands == null

                @Suppress("UNCHECKED_CAST")
                commands = value as List<String>
            }

            judo.onMainThread {
                if (isDebug()) {
                    printDebug("# MSDP: SET($name) = $value")
                }

                judo.events.raise("MSDP", arrayOf(name, value))
                judo.events.raise("MSDP:$name", value)

                if (gotCommands) {
                    judo.events.raise(EVENT_MSDP_ENABLED)
                }
            }
        } else if (isDebug()) {
            printDebug("## TELNET < MSDP: $event")
        }
    }

    override fun onRemoteWill(client: TelnetClient) {
        super.onRemoteWill(client)

        printDebug("## TELNET > IAC SB MSDP MSDP_VAR 'LIST' MSDP_VAL '$INITIAL_LIST' IAC SE")
        client.sendSubnegotiation {
            write(MSDP_LIST_COMMANDS)
        }
    }
}

fun buildMsdpRequest(key: String, value: String) =
    ByteArrayOutputStream(3 + key.length + value.length).apply {
        write(MSDP_VAR)
        write(key)
        write(MSDP_VAL)
        write(value)
    }.toByteArray()

class MsdpReader() {
    private lateinit var event: TelnetEvent
    private var index = 0

    constructor(event: TelnetEvent, start: Int) : this() {
        reset(event, start)
    }

    fun readObject(): Any {
        if (event[index] != MSDP_VAL) {
            throw IllegalStateException("Expected MSDP_VAL but was ${event[index]}")
        }

        return when (event[++index]) {
            MSDP_ARRAY_OPEN -> {
                ++index
                readArray()
            }

            MSDP_TABLE_OPEN -> {
                ++index
                readTable()
            }

            else -> readString()
        }
    }

    internal fun readArray(): List<Any> {
        val result = ArrayList<Any>()

        while (event[index] != MSDP_ARRAY_CLOSE) {
            result.add(readObject())
        }

        return result
    }

    internal fun readString(): String {
        val start = index
        val limit = event.length
        while (index < limit) {
            val byte = event[index]
            if (
                byte == MSDP_VAL
                || byte == MSDP_VAR
                || byte == MSDP_ARRAY_CLOSE
                || byte == MSDP_TABLE_CLOSE
            ) {
                break
            }

            ++index
        }

        return event.toString(start, index - start)
    }

    internal fun readTable(): Map<String, Any> {
        val result = HashMap<String, Any>()

        while (event[index] != MSDP_TABLE_CLOSE) {
            if (event[index] != MSDP_VAR) {
                throw IllegalStateException("Expected MSDP_VAR; saw ${event[index]}")
            }

            ++index
            val key = readString()
            val value = readObject()
            result[key] = value
        }

        return result
    }

    fun reset(event: TelnetEvent, start: Int) {
        this.event = event
        this.index = start
    }
}