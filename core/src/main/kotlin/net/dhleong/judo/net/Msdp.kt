package net.dhleong.judo.net

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.event.EVENT_MSDP_ENABLED
import org.apache.commons.net.telnet.TelnetOptionHandler
import java.io.FilterInputStream
import java.io.InputStream
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.charset.Charset


val TELNET_TELOPT_MSDP = 69.toByte()

val MSDP_VAR = 1
val MSDP_VAL = 2

val MSDP_TABLE_OPEN = 3
val MSDP_TABLE_CLOSE = 4

val MSDP_ARRAY_OPEN = 5
val MSDP_ARRAY_CLOSE = 6

private val INITIAL_LIST = "COMMANDS"
private val MSDP_LIST_COMMANDS = buildMsdpRequest("LIST", INITIAL_LIST)
private val MSDP_SUBNEGOTIATION_MAX_LENGTH = 8192

class MsdpHandler(
    private val judo: IJudoCore,
    private val isDebug: () -> Boolean,
    private val echoDebug: (String) -> Unit
) : TelnetOptionHandler(
    TELNET_TELOPT_MSDP.toInt(),
    false,
    false,
    false,
    true // accept WILL MSDP
) {

    companion object {
        /**
         * Wrap an InputStream from commons.net such that MSDP can work properly
         * NOTE: Commons.net's TelnetInputStream has a hard-coded limit of 512
         * bytes per subnegotiation, so we have to use HACKS to fix it.
         * At some point we might just want to write our own Telnet library :\
         */
        fun wrap(input: InputStream): InputStream {
            val telnetInputStreamClass = Class.forName("org.apache.commons.net.telnet.TelnetInputStream")
            val suboptField = telnetInputStreamClass.getDeclaredField("__suboption")
            val filterInputStreamIn = FilterInputStream::class.java.getDeclaredField("in")
            val modifiersField = Field::class.java.getDeclaredField("modifiers")

            suboptField.isAccessible = true
            filterInputStreamIn.isAccessible = true
            modifiersField.isAccessible = true

            // make it non-final
            modifiersField.setInt(suboptField, suboptField.modifiers and Modifier.FINAL.inv())

            var stream: InputStream? = input
            while (stream != null && stream::class.java != telnetInputStreamClass) {
                if (stream is FilterInputStream) {
                    stream = filterInputStreamIn.get(stream) as InputStream?
                } else {
                    stream = null
                }
            }

            stream?.let {
                suboptField.set(it, IntArray(MSDP_SUBNEGOTIATION_MAX_LENGTH))
            }

            return input
        }
    }

    val isMsdpEnabled: Boolean
        get() = commands != null

    private val varReader = MsdpReader()
    private var commands: List<String>? = null

    override fun answerSubnegotiation(suboptionData: IntArray, suboptionLength: Int): IntArray? {
        // NOTE suboptionData[0] == MSDP *always*
        if (suboptionData[1] == MSDP_VAR) {

            varReader.reset(suboptionData, 2, suboptionLength)
            val name = varReader.readString()
            val value = varReader.readObject()

            if (isDebug()) {
                echoDebug("## TELNET < MSDP VAR: $name <- $value")
            }

            var gotCommands = false
            if (name == "COMMANDS") {
                gotCommands = commands == null

                @Suppress("UNCHECKED_CAST")
                commands = value as List<String>
            }

            judo.onMainThread {
                if (isDebug()) {
                    echoDebug("# MSDP: SET($name) = $value")
                }

                judo.events.raise("MSDP", arrayOf(name, value))
                judo.events.raise("MSDP:$name", value)

                if (gotCommands) {
                    judo.events.raise(EVENT_MSDP_ENABLED)
                }
            }
        } else if (isDebug()) {
            val asString = suboptionData.take(suboptionLength).map {
                if (it < 10) ' '.toByte()
                else it.toByte()
            }.toByteArray().toString(Charset.defaultCharset()).trim()
            echoDebug("## TELNET < MSDP: $asString")
        }

        return null
    }

    override fun startSubnegotiationRemote(): IntArray {
        echoDebug("## TELNET > IAC SB MSDP MSDP_VAR 'LIST' MSDP_VAL '$INITIAL_LIST' IAC SE")
        return MSDP_LIST_COMMANDS
    }
}

fun buildMsdpRequest(key: String, value: String) =
    with(ArrayList<Int>(3 + key.length + value.length)) {
        add(TELNET_TELOPT_MSDP.toInt())
        add(MSDP_VAR)
        addAll(key.map { it.toInt() })
        add(MSDP_VAL)
        addAll(value.map { it.toInt() })
        toIntArray()
    }

class MsdpReader() {
    private lateinit var data: IntArray
    private var index = 0
    private var end = 0
    private val stringBuilder = StringBuilder(512)

    constructor(data: IntArray, start: Int, end: Int) : this() {
        reset(data, start, end)
    }

    fun readObject(): Any {
        if (data[index] != MSDP_VAL) {
            throw IllegalStateException("Expected MSDP_VAL but was ${data[index]}")
        }

        return when (data[++index]) {
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

        while (data[index] != MSDP_ARRAY_CLOSE) {
            result.add(readObject())
        }

        return result
    }

    internal fun readString(): String {
        stringBuilder.setLength(0)
        while (index < end) {
            val byte = data[index]
            if (byte == MSDP_VAL
                    || byte == MSDP_VAR
                    || byte == MSDP_ARRAY_CLOSE
                    || byte == MSDP_TABLE_CLOSE) {
                break
            }

            stringBuilder.append(byte.toChar())
            ++index
        }

        return stringBuilder.toString()
    }

    internal fun readTable(): Map<String, Any> {
        val result = HashMap<String, Any>()

        while (data[index] != MSDP_TABLE_CLOSE) {
            if (data[index] != MSDP_VAR) {
                throw IllegalStateException("Expected MSDP_VAR; saw ${data[index]}")
            }

            ++index
            val key = readString()
            val value = readObject()
            result[key] = value
        }

        return result
    }

    fun reset(data: IntArray, start: Int, end: Int) {
        this.data = data
        this.index = start
        this.end = end
    }
}