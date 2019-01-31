package net.dhleong.judo.net

const val TELNET_IAC = 255.toByte()
const val TELNET_SB = 250.toByte()
const val TELNET_SE = 240.toByte()

const val TELNET_TELOPT_MSDP = 69.toByte()
const val TELNET_TELOPT_MCCP2 = 86.toByte()
const val TELNET_TELOPT_GMCP = 201.toByte()

const val MSDP_VAR = 1.toByte()
const val MSDP_VAL = 2.toByte()

fun isTelnetSubsequence(line: CharSequence) =
    line.length >= 3
        && line[0].toByte() == TELNET_IAC
        && line[1].toByte() == TELNET_SB
        && line.last().toByte() == TELNET_SE
