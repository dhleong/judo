package net.dhleong.judo.render

import net.dhleong.judo.util.CircularArrayList
import okio.buffer
import okio.source
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile

/**
 * @author dhleong
 */
class DiskBackedList(
    private val file: File,
    private val maxCapacity: Int = JudoBuffer.DEFAULT_SCROLLBACK_SIZE
) : AbstractList<FlavorableCharSequence>(), Closeable {

    private val loadedLines = CircularArrayList<FlavorableCharSequence>(
        maxCapacity = maxCapacity,
        initialCapacity = 128   // scrolling back is probably not super common,
                                // so let's not pre-allocate more than a couple
                                // pages' worth up front
    )
    private val addedLines = CircularArrayList<FlavorableCharSequence>(maxCapacity)

    private val raf = RandomAccessFile(file, "rw")

    private val linesInFile by lazy {
        var count = 0
        FileInputStream(file).source().buffer().use {
            while (true) {
                val end = it.indexOf('\n'.toByte())
                if (end < 0) break
                it.skip(end + 1)
                ++count
            }
        }
        count.coerceAtMost(maxCapacity)
    }

    override val size: Int
        get() = (linesInFile + addedLines.size).coerceAtMost(maxCapacity)

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
        // TODO can we do this in a buffer?
        val lineEnd = lastLineEndOffset
        var startOffset = lastLineEndOffset - 1
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

    override fun close() {
        raf.close()
    }

}