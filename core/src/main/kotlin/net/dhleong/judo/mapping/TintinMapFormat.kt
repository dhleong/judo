package net.dhleong.judo.mapping

import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import kotlin.math.ceil

private const val BASE_MAP_CAPACITY = 50000

/**
 * A MapFormat that can read and write in a format compatible with TinTin++
 *
 * @author dhleong
 */
class TintinMapFormat(utf8: Boolean = true) : MapFormat {

    override val name = "tintin"

    // FIXME TODO easier way to specify colors
    val exitColor = 278
    val hereColor = 118
    val pathColor = 138
    val roomColor = 178
    val backColor = 0

    val legend =
        if (utf8) mutableListOf(
            String(byteArrayOf(0xE2.toByte(), 0x80.toByte(), 0xA2.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x95.toByte(), 0xB9.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x95.toByte(), 0xBA.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x94.toByte(), 0x97.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x95.toByte(), 0xBB.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x94.toByte(), 0x83.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x94.toByte(), 0x8F.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x94.toByte(), 0xA3.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x95.toByte(), 0xB8.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x94.toByte(), 0x9B.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x94.toByte(), 0x81.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x94.toByte(), 0xBB.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x94.toByte(), 0x93.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x94.toByte(), 0xAB.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x94.toByte(), 0xB3.toByte())),
            String(byteArrayOf(0xE2.toByte(), 0x95.toByte(), 0x8B.toByte())),
            "X"
        )
        else mutableListOf(
            "*", "#", "#", "+", "#", "|", "+", "+",
            "#", "+", "-", "+", "+", "+", "+", "+",
            "x"
        )

    override fun read(map: IJudoMap, input: InputStream) {
        input.bufferedReader().forEachLine { line ->
            if (line.isEmpty()) return@forEachLine

            when (line[0]) {
                'I' -> map.inRoom = line.substring(2).toInt()
                'R' -> {
                    val args = line.splitArgsInBraces().iterator()
                    val id = args.next().trim().toInt()
                    args.next() // flags
                    args.next() // color
                    val name = args.next()
                    val existing = map[id] as JudoRoom?
                    if (existing == null) {
                        map.add(JudoRoom(id, name))
                    } else {
                        existing.name = name
                    }
                }
                'E' -> {
                    val room = map[map.lastRoom] ?: throw IllegalStateException()

                    val args = line.splitArgsInBraces().iterator()
                    val dest = args.next().trim().toInt()
                    val name = args.next()
                    val cmd = args.next()
                    args.next() // dirs
                    val flags = args.next().trim().toInt()

                    val destRoom = map[dest] ?: stubRoom(map, dest)
                    room.exits[cmd] = JudoExit(destRoom, name, cmd, flags)
                }
            }
        }
    }

    private fun stubRoom(map: IJudoMap, roomId: Int): IJudoRoom {
        val room = JudoRoom(roomId, "<stub>")
        map.add(room)
        return room
    }

    override fun write(map: IJudoMap, out: OutputStream) {
        val mapCapacity: Int = (ceil(map.size / BASE_MAP_CAPACITY.toDouble()) * BASE_MAP_CAPACITY).toInt()

        with(PrintWriter(out.bufferedWriter())) {
            println("C $mapCapacity")
            println()
            printColor("E", exitColor)
            printColor("H", hereColor)
            printColor("P", pathColor)
            printColor("R", roomColor)
            printColor("B", backColor)
            println()
            println("F 8\n") // "Flags"; 8 = ascii graphics
            println("I ${map.inRoom ?: map.lastRoom}\n")

            print("L ")
            legend.joinTo(this, " ")
            println("\n")

            // rooms and exits
            map.forEach { room ->
                val roomId = "%5d".format(room.id)
                println("\nR {$roomId} {0} {} {${room.name}} { } {} {} {} {} {} {1.000}")

                room.exits.forEach { exit ->
                    val exitId = "%5d".format(exit.room.id)
                    println("E {$exitId} {${exit.name}} {${exit.cmd}} {${exit.dirs}} {${exit.flags}} {}")
                }
            }

            flush()
        }
    }
}

internal fun CharSequence.splitArgsInBraces(): Sequence<String> {
    var start = indexOf('{')
    return generateSequence {
        if (start == -1) null
        else {
            val end = indexOf('}', startIndex = start)
            val arg = substring(start + 1, end)

            // onMove along
            start = indexOf('{', startIndex = end)

            // return the arg
            arg
        }
    }
}

private fun PrintWriter.printColor(kind: String, color: Int) =
    if (color == 0) this.write("C$kind ")
    else println("C$kind <$color>")

