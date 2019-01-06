package net.dhleong.judo.net

const val TELNET_IAC = 255.toByte()
const val TELNET_SB = 250.toByte()
const val TELNET_SE = 240.toByte()

const val TELNET_TELOPT_TERMINAL_TYPE = 24.toByte()
const val TELNET_TELOPT_MSDP = 69.toByte()
const val TELNET_TELOPT_MCCP2 = 86.toByte()

const val TELNET_TELOPT_GMCP = 201

const val MSDP_VAR = 1
const val MSDP_VAL = 2

const val MSDP_TABLE_OPEN = 3
const val MSDP_TABLE_CLOSE = 4

const val MSDP_ARRAY_OPEN = 5
const val MSDP_ARRAY_CLOSE = 6

const val MTTS_ANSI = 1
const val MTTS_VT100 = 2
const val MTTS_UTF8 = 4
const val MTTS_256COLOR = 8

const val TELNET_IS = 0.toByte()
const val TELNET_DO = 1.toByte()

fun isTelnetSubsequence(line: CharSequence) =
    line.length >= 3
        && line[0].toByte() == TELNET_IAC
        && line[1].toByte() == TELNET_SB
        && line.last().toByte() == TELNET_SE
