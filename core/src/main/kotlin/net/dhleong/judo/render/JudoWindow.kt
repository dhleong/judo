package net.dhleong.judo.render

import net.dhleong.judo.StateMap
import net.dhleong.judo.WORD_WRAP
import net.dhleong.judo.util.ansi
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder

/**
 * @author dhleong
 */
class JudoWindow(
    override val id: Int,
    private val settings: StateMap,
    initialWidth: Int,
    initialHeight: Int,
    initialBuffer: IJudoBuffer
) : IJudoWindow {

    override var width: Int = initialWidth
    override var height: Int = initialHeight

    override var currentBuffer: IJudoBuffer = initialBuffer

    private var displayWorkspace = ArrayList<AttributedString>(initialHeight + 8)

    /** offset from end of output */
    private var scrollbackBottom = 0

    /** offset within output[scrollbackBottom] */
    private var scrollbackOffset = 0

    private var lastSearchKeyword: String = ""
    private var searchResultLine = -1

    override fun appendLine(line: CharSequence, isPartialLine: Boolean): CharSequence {
        val buffer = currentBuffer
        val previousLength = buffer.size
        val wordWrap = WORD_WRAP.read(settings)
        val result = buffer.appendLine(line, isPartialLine, width, wordWrap)
        val linesAdded = buffer.size - previousLength

        val atBottom = scrollbackBottom == 0 && scrollbackOffset == 0
        if (!atBottom) {
            scrollbackBottom += linesAdded
        }

        return result
    }

    override fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    override fun getDisplayLines(lines: MutableList<CharSequence>) {
        val buffer = currentBuffer
        val wordWrap = WORD_WRAP.read(settings)
        val start = buffer.lastIndex - scrollbackBottom
        val end = maxOf(0, start - height)

        displayWorkspace.clear()

        // iterating from oldest to newest
        @Suppress("LoopToCallChain") // no extra allocations please, thanks
        for (i in end..start) {
            val line = buffer[i] as OutputLine
            displayWorkspace.addAll(
                line.getDisplayLines(width, wordWrap)
            )
        }

        val displayEnd = displayWorkspace.size - scrollbackOffset
        val displayStart = displayEnd - height

        val listStart = lines.size
        @Suppress("LoopToCallChain") // no extra allocations please, thanks
        for (i in displayStart..displayEnd) {
            lines.add(displayWorkspace[i])
        }

        if (searchResultLine >= 0) {
            val lineIndex = listStart + searchResultLine
            val original = lines[lineIndex]
            val wordStart = original.indexOf(lastSearchKeyword, ignoreCase = true)
            if (wordStart >= 0) {
                val wordEnd = wordStart + lastSearchKeyword.length
                val word = original.substring(wordStart, wordEnd)
                val highlighted = "${ansi(inverse = true)}$word${ansi(0)}"

                val new = AttributedStringBuilder(original.length)
                new.append(original, 0, wordStart)
                new.appendAnsi(highlighted)
                new.append(original, wordEnd, original.length)
                val newString = new.toAttributedString()

                lines[lineIndex] = newString
            }
        }
    }

}