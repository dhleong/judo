package net.dhleong.judo.net.options

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoCore
import net.dhleong.judo.event.EVENT_GMCP_ENABLED
import net.dhleong.judo.net.TELNET_TELOPT_GMCP
import net.dhleong.judo.net.TelnetClient
import net.dhleong.judo.net.TelnetEvent
import net.dhleong.judo.net.TelnetOptionHandler
import net.dhleong.judo.net.indexOf
import net.dhleong.judo.net.indexOfFirst
import net.dhleong.judo.net.write
import net.dhleong.judo.util.Json
import okio.Okio
import java.io.ByteArrayOutputStream
import java.io.EOFException

class GmcpHandler(
    private val judo: IJudoCore,
    private val isDebug: () -> Boolean,
    private val printDebug: (String) -> Unit
) : TelnetOptionHandler(
    TELNET_TELOPT_GMCP,
    acceptRemoteWill = true // accept WILL GMCP
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

    override fun onSubnegotiation(client: TelnetClient, event: TelnetEvent) {
        // NOTE suboptionData[0] == SB *always*
        //  and suboptionData[1] == MSDP *always*
        val packageStart: Int = event.indexOfFirst(startIndex = 2) { it != ' '.toByte() }
        val spaceSeparator = event.indexOf(' '.toByte(), startIndex = packageStart)
        val dataStart: Int
        val packageEnd: Int
        if (spaceSeparator < 0 || spaceSeparator >= event.length) {
            // just the package name
            packageEnd = event.length
            dataStart = -1
        } else {
            packageEnd = spaceSeparator
            dataStart = event.indexOfFirst(
                startIndex = spaceSeparator
            ) { it != ' '.toByte() }
        }

        val packageName = event.toString(packageStart, packageEnd - packageStart)

        // parse data (if there is any)
        val data =
            if (dataStart == -1 || dataStart >= event.length) null
            else {
                try {
                    Json.read<Any>(
                        Okio.source(event.toInputStream(
                            packageEnd + 1,
                            event.length - packageEnd - 1
                        ))
                    )
                } catch (e: EOFException) {
                    null
                }
            }

        judo.onMainThread {
            if (isDebug()) {
                printDebug("# GMCP: `$packageName` = `$data`")
            }

            judo.events.raise("GMCP", arrayOf(packageName, data))
            judo.events.raise("GMCP:$packageName", data)
        }
    }

    override fun onRemoteWill(client: TelnetClient) {
        super.onRemoteWill(client)

        printDebug("## TELNET > IAC SB GMCP Core.Hello ...")

        isGmcpEnabled = true

        judo.onMainThread {
            judo.events.raise(EVENT_GMCP_ENABLED)
        }

        client.sendSubnegotiation {
            write(GMCP_HELLO)
        }
    }

    fun buildGmcpRequest(packageName: String, value: Any?) =
        ByteArrayOutputStream(packageName.length + 32).apply {
            write(packageName)
            if (value != null) {
                write(' '.toByte())
                write(Json.write(value))
            }
        }.toByteArray()
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
