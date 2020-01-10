package net.dhleong.judo.render

import net.dhleong.judo.DelegateStateMap
import net.dhleong.judo.IStateMap
import net.dhleong.judo.render.flavor.flavor
import net.dhleong.judo.util.CircularArrayList
import java.io.File
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

open class JudoBuffer(
    override val id: Int,
    settings: IStateMap,
    private val scrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE
) : IJudoBuffer {

    constructor(
        ids: IdManager,
        settings: IStateMap,
        scrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE
    ) : this(ids.newBuffer(), settings, scrollbackSize)

    private var contents: BufferStorage<FlavorableCharSequence> = CircularArrayList(scrollbackSize)
    private val windows = mutableListOf<IJudoWindow>()

    override val settings: IStateMap = DelegateStateMap(settings)

    override val size: Int
        get() = contents.size
    override val lastIndex: Int
        get() = contents.lastIndex

    override fun get(index: Int): FlavorableCharSequence = contents[index]

    @Synchronized
    override fun append(text: FlavorableCharSequence) = notifyingChanges {
        text.splitAtNewlines(contents, continueIncompleteLines = true)
    }

    @Synchronized
    override fun appendLine(line: FlavorableCharSequence) = notifyingChanges {
        if (!line.endsWith('\n')) {
            line += '\n'
        }
        line.splitAtNewlines(contents, continueIncompleteLines = false)
    }

    @Synchronized
    override fun clear() = notifyingChanges{
        contents.clear()
    }

    @Synchronized
    override fun deleteLast() = notifyingChanges {
        contents.removeLast()
    }

    @Synchronized
    override fun replaceLastLine(result: FlavorableCharSequence) = notifyingChanges {
        contents[contents.lastIndex] = result
    }

    @Synchronized
    override fun set(newContents: List<FlavorableCharSequence>) = notifyingChanges {
        clear()
        newContents.forEach(this::appendLine)
    }

    @Synchronized
    override fun set(index: Int, line: FlavorableCharSequence) = notifyingChanges {
        val newLine = line.indexOf('\n')
        require(newLine == -1 || newLine == line.lastIndex) {
            "Line must not have any newline characters in it"
        }
        if (!line.endsWith("\n")) {
            line += '\n'
        }
        contents[index] = line
    }

    override fun setPersistent(file: File) {
        val old = contents
        if (old is DiskBackedList) {
            throw IllegalStateException("Buffer#$id is already persistent")
        }

        val newList = DiskBackedList(file, maxCapacity = scrollbackSize)
        contents = newList

        if (newList.isNotEmpty()) {
            appendLine("^^^ Loaded ${newList.size} lines at ${Date()}\n".withFlavor(flavor(
                isInverse = true
            )))
            appendLine("".toFlavorable())
        }
    }

    override fun setNotPersistent() {
        contents.save()
        val old = contents
        if (old !is CircularArrayList) {
            contents = CircularArrayList(scrollbackSize)
            contents.addAll(old)
        }
    }

    override fun attachWindow(window: IJudoWindow) {
        windows += window
    }

    override fun detachWindow(window: IJudoWindow) {
        windows -= window
    }

    private val changeWorkspace = arrayListOf<Any?>()
    private val changeDepth = AtomicInteger(0)
    protected fun beginChange() {
        if (changeDepth.getAndIncrement() > 0) return

        changeWorkspace.ensureCapacity(windows.size)
        for (i in changeWorkspace.size until windows.size) {
            changeWorkspace.add(null)
        }

        for (i in windows.indices) {
            changeWorkspace[i] = windows[i].onBufModifyPre()
        }
    }
    protected fun endChange() {
        if (changeDepth.decrementAndGet() > 0) return

        for (i in windows.indices) {
            windows[i].onBufModifyPost(changeWorkspace[i])
        }
        changeWorkspace.fill(null)
    }

    protected inline fun <R> notifyingChanges(block: () -> R): R = try {
        beginChange()
        block()
    } finally {
        endChange()
    }

    companion object {
        const val DEFAULT_SCROLLBACK_SIZE = 20_000
    }
}