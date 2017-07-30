package net.dhleong.judo.net

import com.squareup.moshi.Moshi
import net.dhleong.judo.IJudoCore
import okio.Okio
import org.apache.commons.net.telnet.TelnetOptionHandler
import java.io.ByteArrayInputStream

val TELNET_TELOPT_GMCP = 201.toByte()

class GmcpHandler(
    private val judo: IJudoCore,
    private val isDebug: () -> Boolean,
    private val echoDebug: (String) -> Unit
) : TelnetOptionHandler(
    TELNET_TELOPT_GMCP.toInt(),
    false,
    false,
    false,
    true // accept WILL GMCP
) {

    val moshi = Moshi.Builder().build()

    override fun answerSubnegotiation(suboptionData: IntArray, suboptionLength: Int): IntArray? {
        val spaceSeparator = suboptionData.indexOf(' '.toInt())
        val dataStart: Int
        val packageEnd: Int
        if (spaceSeparator < 0 || spaceSeparator >= suboptionLength) {
            // just the package name
            packageEnd = suboptionLength
            dataStart = -1
        } else {
            packageEnd = spaceSeparator
            dataStart = spaceSeparator + 1
        }

        val suboptionByteArray = suboptionData
            .map { it.toByte() }
            .toByteArray()
        val packageName = String(suboptionByteArray, 0, packageEnd)

        // parse data (if there is any)
        val data =
            if (dataStart == -1) null
            else {
                moshi.adapter(Any::class.java).fromJson(
                    Okio.buffer(
                        Okio.source(ByteArrayInputStream(
                            suboptionByteArray,
                            packageEnd + 1,
                            suboptionLength - packageEnd - 1
                        ))
                    )
                )
            }

        judo.onMainThread {
            if (isDebug()) {
                echoDebug("# GMCP: $packageName = $data")
            }

            judo.events.raise("GMCP", arrayOf(packageName, data))
            judo.events.raise("GMCP:$packageName", data)
        }

        return null
    }
}
