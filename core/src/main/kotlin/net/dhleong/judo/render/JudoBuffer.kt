package net.dhleong.judo.render

import net.dhleong.judo.util.CircularArrayList


private val DEFAULT_SCROLLBACK_SIZE = 20_000

/**
 * @author dhleong
 */
class JudoBuffer(
    ids: IdManager,
    scrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE
) : IJudoBuffer {

    override val id = ids.newBuffer()

    override val size: Int
        get() = output.size

    override val lastIndex: Int
        get() = output.lastIndex

    private val output = CircularArrayList<OutputLine>(scrollbackSize)

    private var hadPartialLine = false

    @Synchronized override fun appendLine(
        line: CharSequence, isPartialLine: Boolean,
        windowWidthHint: Int, wordWrap: Boolean
    ): CharSequence {
        val outputLine = line as? OutputLine ?: OutputLine(line)
        val result = appendOutputLineInternal(outputLine, isPartialLine, windowWidthHint, wordWrap)
        hadPartialLine = isPartialLine
        return result
    }

    override fun get(index: Int): CharSequence = output[index]

    @Synchronized override fun replaceLastLine(result: CharSequence) {
        // TODO remove the line completely if empty?
        // TODO split line?
        output[output.lastIndex] = when (result) {
            is OutputLine -> result
            else -> OutputLine(result)
        }
    }

    @Synchronized override fun clear() {
        output.clear()
    }

    override fun set(newContents: List<CharSequence>) {
        output.clear()
        @Suppress("LoopToCallChain") // no extra allocations, please
        for (line in newContents) {
            output.add(line as? OutputLine ?: OutputLine(line))
        }
    }

    private fun appendOutputLineInternal(
        line: OutputLine, isPartialLine: Boolean,
        windowWidthHint: Int, wordWrap: Boolean
    ): OutputLine {
        val splitCandidate: OutputLine
        if (hadPartialLine) {
            hadPartialLine = false

            // merge partial line
            val original = output.removeLast()
            original.append(line)

            splitCandidate = original
        } else {
            // new line
            if (output.isNotEmpty()) {
                val previous = output.last()
                line.setStyleHint(previous.getFinalStyle())
            }
            splitCandidate = line
        }

        if (isPartialLine) {
            // never split partial lines right away
            output.add(splitCandidate)
        } else {
            // full line. split away!
            val split = splitCandidate.getDisplayOutputLines(windowWidthHint, wordWrap)
            split.forEach(output::add)
        }

        return splitCandidate
    }

}
