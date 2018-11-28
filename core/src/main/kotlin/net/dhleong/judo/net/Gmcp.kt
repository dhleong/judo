package net.dhleong.judo.net

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoCore
import net.dhleong.judo.event.EVENT_GMCP_ENABLED
import net.dhleong.judo.util.Json
import okio.Okio
import org.apache.commons.net.telnet.TelnetOptionHandler
import java.io.ByteArrayInputStream
import java.io.EOFException

const val TELNET_TELOPT_GMCP = 201

class GmcpHandler(
    private val judo: IJudoCore,
    private val isDebug: () -> Boolean,
    private val echoDebug: (String) -> Unit
) : TelnetOptionHandler(
    TELNET_TELOPT_GMCP,
    false,
    false,
    false,
    true // accept WILL GMCP
) {

    var isGmcpEnabled = false
        private set

    private val GMCP_HELLO = buildGmcpRequest(
        "Core.Hello",
        mapOf(
            "client" to JudoCore.CLIENT_NAME,
            "version" to JudoCore.CLIENT_VERSION
        )
    )

    override fun answerSubnegotiation(suboptionData: IntArray, suboptionLength: Int): IntArray? {
        // NOTE suboptionData[0] == MSDP *always*
        val packageStart: Int = suboptionData.indexOfFirst(startIndex = 1) { it != ' '.toInt() }
        val spaceSeparator = suboptionData.indexOf(' '.toInt(), startIndex = packageStart)
        val dataStart: Int
        val packageEnd: Int
        if (spaceSeparator < 0 || spaceSeparator >= suboptionLength) {
            // just the package name
            packageEnd = suboptionLength
            dataStart = -1
        } else {
            packageEnd = spaceSeparator
            dataStart = suboptionData.indexOfFirst(
                startIndex = spaceSeparator
            ) { it != ' '.toInt() }
        }

        val suboptionByteArray = suboptionData
            .map { it.toByte() }
            .toByteArray()
        val packageName = String(
            suboptionByteArray,
            packageStart,
            packageEnd - packageStart
        )

        // parse data (if there is any)
        val data =
            if (dataStart == -1 || dataStart >= suboptionLength) null
            else {
                try {
                    Json.read<Any>(
                        Okio.source(ByteArrayInputStream(
                            suboptionByteArray,
                            packageEnd + 1,
                            suboptionLength - packageEnd - 1
                        ))
                    )
                } catch (e: EOFException) {
                    null
                }
            }

        judo.onMainThread {
            if (isDebug()) {
                echoDebug("# GMCP: `$packageName` = `$data`")
            }

            judo.events.raise("GMCP", arrayOf(packageName, data))
            judo.events.raise("GMCP:$packageName", data)
        }

        return null
    }

    override fun startSubnegotiationRemote(): IntArray? {
        echoDebug("## TELNET > IAC SB GMCP Core.Hello ...")

        isGmcpEnabled = true

        judo.onMainThread {
            judo.events.raise(EVENT_GMCP_ENABLED)
        }

        return GMCP_HELLO
    }

    fun buildGmcpRequest(packageName: String, value: Any?) =
        with(ArrayList<Int>(packageName.length + 32)) {
            add(TELNET_TELOPT_MSDP.toInt())
            addAll(packageName.map { it.toInt() })
            if (value != null) {
                add(' '.toInt())
                addAll(
                    Json.write(value)
                        .map { it.toInt() })
            }
            toIntArray()
        }
}

fun IntArray.indexOf(element: Int, startIndex: Int = 0): Int {
    @Suppress("LoopToCallChain")
    for (index in startIndex..(size - 1)) {
        if (element == this[index]) {
            return index
        }
    }
    return -1
}

inline fun IntArray.indexOfFirst(startIndex: Int = 0, predicate: (Int) -> Boolean): Int {
    @Suppress("LoopToCallChain")
    for (index in startIndex..(size - 1)) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}
