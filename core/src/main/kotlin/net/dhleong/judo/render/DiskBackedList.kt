package net.dhleong.judo.render

import net.dhleong.judo.net.toAnsi
import net.dhleong.judo.util.CircularArrayList
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * @author dhleong
 */
class DiskBackedList(
    private val file: File,
    private val maxCapacity: Int = JudoBuffer.DEFAULT_SCROLLBACK_SIZE
) : AbstractMutableList<FlavorableCharSequence>(),
    BufferStorage<FlavorableCharSequence>
{

    init {
        val parent = file.absoluteFile.parentFile
        if (!(parent.isDirectory || parent.mkdirs())) {
            throw IllegalArgumentException("Unable to persist to ${file.absolutePath}")
        }
    }

    private val loadedLines = CircularArrayList<FlavorableCharSequence>(
        maxCapacity = maxCapacity,
        initialCapacity = 128   // scrolling back is probably not super common,
                                // so let's not pre-allocate more than a couple
                                // pages' worth up front
    )
    private val addedLines = CircularArrayList<FlavorableCharSequence>(maxCapacity)

    private var knownLinesInFile: Int = -1
    private val linesInFile: Int get() = synchronized(this) {
        val known = knownLinesInFile
        val actualLinesInFile = if (known >= 0) known
        else {
            val counted = countLinesInFile()
            knownLinesInFile = counted
            counted
        }

        // it's possible to have >maxCapacity by combining addedLines and the lines
        // on disk. to ensure we load from addedLines as we approach this point
        // (creating a sort of composite CircularArrayList) we reduce the number
        // of lines we claim are in the file
        val addedLines = addedLines.size
        val extra = maxCapacity - addedLines - actualLinesInFile
        if (extra >= 0) {
            actualLinesInFile
        } else {
            actualLinesInFile + extra
        }
    }

    override val size: Int
        @Synchronized
        get() = (linesInFile + addedLines.size).coerceAtMost(maxCapacity)

    @Synchronized
    override fun get(index: Int): FlavorableCharSequence =
        if (index < linesInFile) {
            getLineInFile(index)
        } else {
            addedLines[index - linesInFile]
        }

    private var lastLineEndOffset = file.length() - 1

    private fun getLineInFile(index: Int): FlavorableCharSequence {
        val virtualIndex = linesInFile - index - 1
        if (virtualIndex >= maxCapacity) {
            throw IndexOutOfBoundsException()
        }

        if (virtualIndex >= loadedLines.size) {
            // load lines going backward
            synchronized(file) {
                val linesToRead = virtualIndex - loadedLines.size + 1
                for (i in 0 until linesToRead) {
                    val l = readNextLineBackwards()
                    loadedLines += l.parseAnsi()
                }
            }
        }

        return loadedLines[virtualIndex]
    }

    private fun readNextLineBackwards(): String {
        // TODO can/should we do this in a buffer?
        val lineEnd = lastLineEndOffset
        var startOffset = lastLineEndOffset - 1

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(startOffset)
            while (startOffset > 0) {
                val b = raf.read()
                if (b == '\n'.toInt()) break
                raf.seek(--startOffset)
            }

            lastLineEndOffset = startOffset
            val bytes = ByteArray((lineEnd - startOffset).toInt())
            raf.read(bytes)
            return String(bytes, Charsets.UTF_8)
        }
    }

    override fun add(index: Int, element: FlavorableCharSequence) {
        throw UnsupportedOperationException()
    }

    @Synchronized
    override fun add(element: FlavorableCharSequence): Boolean {
        if (addedLines.size == maxCapacity) {
            loadedLines.add(0, addedLines.removeFirst())
        }
        addedLines += element
        return true
    }

    @Synchronized
    override fun clear() {
        knownLinesInFile = 0
        loadedLines.clear()
        addedLines.clear()
        file.delete()
        lastLineEndOffset = 0
    }

    override fun removeAt(index: Int): FlavorableCharSequence {
        throw UnsupportedOperationException()
    }

    @Synchronized
    override fun set(index: Int, element: FlavorableCharSequence): FlavorableCharSequence =
        if (index < linesInFile) {
            TODO("modify lines on disk?")
        } else {
            // TODO auto-persist?
            val actualIndex = index - linesInFile
            addedLines.set(actualIndex, element)
        }

    @Synchronized
    override fun removeLast(): FlavorableCharSequence {
        if (addedLines.isNotEmpty()) {
            // TODO auto-persist?
            return addedLines.removeLast()
        }

        throw UnsupportedOperationException("Unable to remove old, persisted lines")
    }

    override fun save() {
        flush()
    }

    @Synchronized
    fun flush(maxLinesToFlush: Int = Int.MAX_VALUE) {
        val linesToFlush = addedLines.size.coerceAtMost(maxLinesToFlush)
        val linesFromOld = (maxCapacity - linesToFlush).coerceAtMost(linesInFile)
        val oldLineIndex = linesInFile - linesFromOld
        val offset = file.offsetOfNth('\n', oldLineIndex)

        val tmpFile = File(file.absolutePath + ".swp")
        if (offset >= 0) {
            FileOutputStream(tmpFile).channel.use { output ->
                FileInputStream(file).channel.use { input ->
                    input.transferTo(offset, Long.MAX_VALUE, output)
                }
            }
        }

        var bytesWritten = 0L
        FileOutputStream(tmpFile, true).sink().buffer().use { output ->
            for ((lineIndex, l) in addedLines.withIndex()) {
                if (lineIndex >= linesToFlush) break

                l.toAnsi().byteInputStream().source().use {
                    bytesWritten += output.writeAll(it)

                    // it should in general, but make certain:
                    if (!l.endsWith('\n')) {
                        ++bytesWritten
                        output.writeByte('\n'.toInt())
                    }
                }
            }
        }

        // shift the flushed lines from addedLines to loadedLines
        for (i in 0 until linesToFlush) {
            loadedLines.add(0, addedLines.removeFirst())
        }

        lastLineEndOffset -= bytesWritten
        tmpFile.renameTo(file)
        knownLinesInFile = linesFromOld + linesToFlush
    }

    @Synchronized
    private fun countLinesInFile(): Int =
        try {
            var count = 0
            FileInputStream(file).source().buffer().use {
                while (true) {
                    val end = it.indexOf('\n'.toByte())
                    if (end < 0) {
                        if (!it.exhausted()) {
                            // one more line that doesn't end with \n
                            ++count
                        }
                        break
                    }
                    it.skip(end + 1)
                    ++count
                }
            }
            count.coerceAtMost(maxCapacity)
        } catch (e: FileNotFoundException) {
            // doesn't exist? no lines
            0
        }
}

private fun File.offsetOfNth(char: Char, nth: Int): Long = try {
    var count = 0
    var offset = 0L

    FileInputStream(this).source().buffer().use {
        while (count < nth) {
            val index = it.indexOf(char.toByte())
            if (index == -1L) break
            offset += index
            ++count
        }
    }

    offset
} catch (e: FileNotFoundException) {
    -1L
}
