package net.dhleong.judo

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.alias.AliasProcessingException
import net.dhleong.judo.complete.DEFAULT_STOP_WORDS
import net.dhleong.judo.complete.MarkovCompletionSource
import net.dhleong.judo.complete.MultiplexCompletionSource
import net.dhleong.judo.complete.RecencyCompletionSource
import net.dhleong.judo.complete.multiplex.WeightedRandomSelector
import net.dhleong.judo.complete.multiplex.wordsBeforeFactory
import net.dhleong.judo.event.EventManager
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Key
import net.dhleong.judo.input.Keys
import net.dhleong.judo.input.changes.UndoManager
import net.dhleong.judo.logging.LogManager
import net.dhleong.judo.mapping.MapManager
import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.modes.BaseCmdMode
import net.dhleong.judo.modes.InputBufferProvider
import net.dhleong.judo.modes.InsertMode
import net.dhleong.judo.modes.MappableMode
import net.dhleong.judo.modes.NormalMode
import net.dhleong.judo.modes.OperatorPendingMode
import net.dhleong.judo.modes.OutputSearchMode
import net.dhleong.judo.modes.PythonCmdMode
import net.dhleong.judo.modes.ReverseInputSearchMode
import net.dhleong.judo.modes.ScriptExecutionException
import net.dhleong.judo.modes.StatusBufferProvider
import net.dhleong.judo.modes.UserCreatedMode
import net.dhleong.judo.net.CommonsNetConnection
import net.dhleong.judo.net.Connection
import net.dhleong.judo.net.JudoConnection
import net.dhleong.judo.prompt.PromptManager
import net.dhleong.judo.register.RegisterManager
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.render.JudoTabpage
import net.dhleong.judo.render.OutputLine
import net.dhleong.judo.render.PrimaryJudoWindow
import net.dhleong.judo.trigger.TriggerManager
import net.dhleong.judo.util.IStringBuilder
import net.dhleong.judo.util.InputHistory
import net.dhleong.judo.util.ansi
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import java.io.PrintWriter
import java.util.concurrent.ArrayBlockingQueue

/**
 * @author dhleong
 */

enum class DebugLevel {
    OFF,
    NORMAL,
    ALL;

    val isEnabled: Boolean
        get() = this != OFF
}

internal val UNRECORDED_KEY_CODES = arrayOf(
    Key.CODE_ESCAPE,
    Key.CODE_PAGE_UP,
    Key.CODE_PAGE_DOWN
)

class JudoCore(
    override val renderer: JudoRenderer,
    mapRenderer: MapRenderer,
    settings: StateMap,
    val debug: DebugLevel = DebugLevel.OFF
) : IJudoCore {
    companion object {
        val CLIENT_NAME = BuildConfig.NAME
        val CLIENT_VERSION = BuildConfig.VERSION
    }

    override val aliases = AliasManager()
    override val events = EventManager()
    override val logging = LogManager()
    override val mapper = MapManager(this, settings, mapRenderer)
    override val triggers = TriggerManager()
    override val prompts = PromptManager()
    override val registers = RegisterManager(settings)
    override val state = settings

    internal val undo = UndoManager()

    private val parsedPrompts = ArrayList<IStringBuilder>(2)

    internal val buffer = InputBuffer(registers, undo)
    internal val cmdBuffer = InputBuffer()

    private val sendHistory = InputHistory(buffer)
    private val cmdHistory = InputHistory(cmdBuffer)

    // fancy completions from stuff the user input
    private val commandCompletions = MultiplexCompletionSource(
        listOf(
            MarkovCompletionSource(stopWords = DEFAULT_STOP_WORDS),
            RecencyCompletionSource(maxCandidates = 1000)),
        wordsBeforeFactory(WeightedRandomSelector.distributeByWordIndex(
            // the markov trie has a max depth of 5; at that point, we start to suspect
            // that it's not a structured command, so we let recency have more weight
            doubleArrayOf(1.0, .0),
            doubleArrayOf(1.0, .0),
            doubleArrayOf(1.0, .0),
            doubleArrayOf(1.0, .0),

            // after the first few words, still prefer markov, but
            // give recent words a bit of a chance, too
            doubleArrayOf(.5, .5)
        ))
    )

    // completions from stuff the server output
    private val outputCompletions = RecencyCompletionSource()

    // `completions` combines completions from output and from input
    private val completions = MultiplexCompletionSource(
        listOf(commandCompletions, outputCompletions),
        wordsBeforeFactory(WeightedRandomSelector.distributeByWordIndex(
            // first word? prefer commandCompletions ALWAYS;
            // we'll still fallback to output if commandCompletion
            // doesn't have anything
            doubleArrayOf(1.0, .00),

            // second word? actually, prefer output a bit
            // eg: get <thing>; enter <thing>; look <thing>
            doubleArrayOf(.35, .65),

            // otherwise, just split it evenly
            doubleArrayOf(.5, .5)
        ))
    )

    private val opMode = OperatorPendingMode(this, buffer)
    private val normalMode = NormalMode(this, buffer, sendHistory, opMode)

    private val ids = IdManager()

    private val debugLogFile = File("debug-log.txt")

    private val modes = sequenceOf(

        InsertMode(this, buffer, completions, sendHistory),
        normalMode,
        opMode,
        OutputSearchMode(this, outputCompletions),
        PythonCmdMode(this, ids, cmdBuffer, renderer, cmdHistory, completions),
        ReverseInputSearchMode(this, buffer, sendHistory)

    ).fold(HashMap<String, Mode>(), { map, mode ->
        map[mode.name] = mode
        map
    })

    private var currentMode: Mode = normalMode
    private val modeStack = ArrayList<Mode>()

    internal var running = true
    internal var doEcho = true

    override var connection: Connection? = null
    private var lastConnect: Pair<String, Int>? = null

    private val statusLineWorkspace = IStringBuilder.create(128)

    private var keyStrokeProducer: BlockingKeySource? = null
    private val postToUiQueue = ArrayBlockingQueue<() -> Unit>(64)

    private val outputBuffer: IJudoBuffer
    private val primaryWindow: PrimaryJudoWindow
    override val tabpage: IJudoTabpage

    init {
        val rendererTabpage = renderer.currentTabpage
        if (rendererTabpage != null) {
            // NOTE: this should only happen in tests
            tabpage = rendererTabpage
            primaryWindow = tabpage.currentWindow as PrimaryJudoWindow
            outputBuffer = primaryWindow.outputWindow.currentBuffer
        } else {
            outputBuffer = JudoBuffer(ids)
            primaryWindow = PrimaryJudoWindow(
                ids, settings, outputBuffer,
                renderer.windowWidth, renderer.windowHeight - 1)
            tabpage = JudoTabpage(ids, settings, primaryWindow)
        }

        primaryWindow.isFocused = true
        renderer.currentTabpage = tabpage
        renderer.settings = state
        renderer.onResized = {
            renderer.inTransaction {
                updateStatusLine(currentMode)
                updateInputLine()

                rendererTabpage?.currentWindow?.let {
                    mapper.resize(width = it.width)
                }
            }
        }

        if (debug.isEnabled) {
            System.setErr(PrintStream(debugLogFile.outputStream()))
        }

        activateMode(currentMode)
    }

    override fun connect(address: String, port: Int) {
        disconnect()
        appendOutput(OutputLine("Connecting to $address:$port... "))

        lastConnect = address to port

        val connection: Connection
        try {
            connection = CommonsNetConnection(this, address, port, { string -> echo(string) })
            connection.debug = debug.isEnabled
            echo("Connected.")
        } catch (e: IOException) {
            appendError(e, "Failed.\nNETWORK ERROR: ")
            return
        }


        connection.setWindowSize(renderer.windowWidth, renderer.windowHeight)
        connection.onDisconnect = this::onDisconnect
        connection.onEchoStateChanged = { doEcho ->
            this.doEcho = doEcho

            if (debug.isEnabled) {
                echo("## TELNET doEcho($doEcho)")
            }
        }
        connection.onError = {
            appendError(it, "NETWORK ERROR: ")

            // force disconnect on network error; we've probably already
            // lost connection, but in case we haven't, manually handle
            // onDisconnect to avoid duplicates
            connection.onDisconnect = null
            disconnect()
            onDisconnect(connection)
        }
        connection.forEachLine { buffer, count ->
            if (debug == DebugLevel.ALL) {
                FileOutputStream(debugLogFile, true).use {
                    it.bufferedWriter().use {
                        it.write(buffer, 0, count)
                        it.write("{PACKET_BOUNDARY}")
                    }
                }
            }

            onIncomingBuffer(buffer, count)
        }

        this.connection = connection
        events.raise("CONNECTED")
    }

    override fun createUserMode(name: String) {
        modes[name] = UserCreatedMode(this, name)
    }

    override fun disconnect() {
        connection?.let {
            it.onError = null
            it.close()
        }
    }

    override fun echo(vararg objects: Any?) {
        val asString = objects.joinToString(" ")
        doEcho(true, asString)
    }

    override fun echoRaw(vararg objects: Any?) {
        val asString = objects.joinToString(" ")
        doEcho(false, asString)
    }

    private fun doEcho(process: Boolean, asString: String) {
        // TODO colors?
        appendOutput(OutputLine("${ansi(0)}$asString\n"), process = process)

        if (debug.isEnabled) {
            debugLogFile.appendText("\n## ECHO: $asString\n")
        }
    }

    override fun enterMode(modeName: String) {
        val mode = modes[modeName]
        if (mode != null) {
            enterMode(mode)
        } else {
            throw IllegalArgumentException("No such mode `$modeName`")
        }
    }

    override fun enterMode(mode: Mode) {
        if (state[MODE_STACK] &&
            (modeStack.isEmpty() || modeStack.last() != mode)) {
            modeStack.add(currentMode)
        }

        activateMode(mode)
    }

    override fun exitMode() {
        if (currentMode == normalMode) return

        if (state[MODE_STACK] && !modeStack.isEmpty()) {
            // actually, return to the previous mode
            val previousMode = modeStack.removeAt(modeStack.lastIndex)
            activateMode(previousMode)
        } else {
            activateMode(normalMode)
        }
    }

    override fun map(mode: String, from: String, to: String, remap: Boolean) {
        modes[mode]?.let { modeObj ->
            if (modeObj !is MappableMode) {
                throw IllegalArgumentException("$mode does not support mapping")
            }

            val fromKeys = Keys.parse(from)
            val toKeys = Keys.parse(to)
            if (remap) {
                modeObj.userMappings.map(fromKeys, toKeys)
            } else {
                modeObj.userMappings.noremap(fromKeys, toKeys)
            }
            return
        }

        // map in all modes
        if (mode == "") {
            modes.keys
                .filter { modes[it] is MappableMode }
                .forEach { map(it, from, to, remap) }
            return
        }

        throw IllegalArgumentException("No such mode $mode")
    }

    override fun map(mode: String, from: String, to: () -> Unit, description: String) {
        modes[mode]?.let { modeObj ->
            if (modeObj !is MappableMode) {
                throw IllegalArgumentException("$mode does not support mapping")
            }

            val fromKeys = Keys.parse(from)
            modeObj.userMappings.map(fromKeys, { _ -> to() }, description)
            return
        }

        // map in all modes
        if (mode == "") {
            modes.keys
                .filter { modes[it] is MappableMode }
                .forEach { map(it, from, to) }
            return
        }

        throw IllegalArgumentException("No such mode $mode")
    }

    override fun printMappings(mode: String) {
        modes[mode]?.let { modeObj ->
            if (modeObj !is MappableMode) {
                throw IllegalArgumentException("$mode does not support mapping")
            }

            echoRaw(modeObj.userMappings)
            return
        }

        throw IllegalArgumentException("No such mode $mode")
    }

    override fun reconnect() {
        lastConnect?.let { (address, port) ->
            connect(address, port)
            return
        }

        throw IllegalStateException("You have to connect() first")
    }

    override fun scrollPages(count: Int) {
        tabpage.currentWindow.scrollPages(count)
    }

    override fun scrollToBottom() {
        tabpage.currentWindow.scrollToBottom()
    }

    override fun seedCompletion(text: String) {
        commandCompletions.process(text)
    }

    override fun send(text: String, fromMap: Boolean) {
        scrollToBottom()

        var doSend = true
        val toSend: String
        if (fromMap || text.isEmpty()) {
            // NOTE: we don't process text sent from mappings or
            // scripts for aliases. Both would be done with send(),
            // and it doesn't seem to make sense that to get
            // alias-ified (not to mention the potential for unintended
            // recursion). We can revisit this later if it's a problem
            toSend = text
        } else {
            try {
                val processed = aliases.process(text)
                if (!text.isEmpty() && processed.isEmpty()) {
                    // if the original text was empty, it's okay to
                    // send it; if it *became* empty, however, we
                    // probably don't want to (EG: an alias to a
                    // function that sends stuff itself)
                    doSend = false
                }

                toSend = processed.toString()
            } catch (e: AliasProcessingException) {
                appendError(e)
                return
            }
        }

        if (doEcho && !(connection?.isTelnetSubsequence(toSend) ?: false)) {
            // always output what we sent
            // except... don't echo if the server has told us not to
            // (EG: passwords)
            echo(toSend) // TODO color?
        }

        if (!fromMap && !text.isEmpty()) {
            // record it even if we couldn't send it
            sendHistory.push(text)
            sendHistory.resetHistoryOffset() // start back from most recent

            // also complete from sent things
            // (but the original text, not the alias-processed one)
            completions.process(text) // NOTE: we let all completers process commands
        }

        if (doSend) {
            mapper.maybeCommand(toSend)

            connection?.let {
                it.send(toSend)
                return
            }

            appendError(Error("Not connected."))
        }
    }

    override fun feedKey(stroke: Key, remap: Boolean, fromMap: Boolean) {
//        echo("## feedKey($stroke)")
        when (stroke.keyCode) {
            Key.CODE_ESCAPE -> {
                // reset the current register
                registers.resetCurrent()

                // leave the current mode
                if (currentMode != normalMode) {
                    modeStack.clear()
                    activateMode(normalMode)
                }
                return
            }

            Key.CODE_PAGE_UP -> {
                tabpage.currentWindow.scrollLines(1)
                return
            }
            Key.CODE_PAGE_DOWN -> {
                tabpage.currentWindow.scrollLines(-1)
                return
            }
        }

        currentMode.feedKey(stroke, remap, fromMap)

        // NOTE: currentMode might have changed as a result of feedKey
        val newMode = currentMode
        renderer.inTransaction {
            if (newMode is StatusBufferProvider) {
                tabpage.currentWindow.updateStatusLine(
                    newMode.renderStatusBuffer(), newMode.getCursor())
            } else {
                updateInputLine()
            }
        }
    }

    override fun feedKeys(keys: String, remap: Boolean, mode: String) {
        feedKeys(
            Keys.parse(keys).asSequence(),
            remap = remap,
            mode = mode
        )
    }

    override fun feedKeys(keys: Sequence<Key>, remap: Boolean, mode: String) {
        val oldMode = currentMode

        if (mode != "") {
            // NOTE: we don't activateMode() because there's a lot of baggage
            // associated with that, and I don't think Vim does that stuff either
            val newMode = modes[mode] ?: throw IllegalArgumentException("No such mode `$mode`")
            currentMode = newMode
        }

        feedKeys(
            keys.iterator(),
            remap = remap,
            fromMap = true
        )

        if (mode != "") {
            // restore the old mode (but without calling onEnter and making it
            // reset its state)
            currentMode = oldMode
        }
    }

    fun feedKeys(keys: Iterator<Key>, remap: Boolean, fromMap: Boolean) {
        readKeys(object : BlockingKeySource {
            override fun readKey(): Key = keys.next()
        }, remap = remap, fromMap = fromMap) {
            keys.hasNext()
        }
    }

    override fun isConnected(): Boolean = connection != null

    override fun persistInput(file: File) {
        if (file.exists() && !(file.canRead() && file.canWrite())) {
            throw IllegalArgumentException("Cannot read or write $file")
        }
        state[KEY_PERSIST_INPUT_HISTORY_PATH] = file

        if (file.exists()) {
            file.forEachLine {
                commandCompletions.process(it)
                sendHistory.push(it)
            }
        }
    }

    override fun onMainThread(runnable: () -> Unit) {
        // NOTE: wait until there is room in the queue
        postToUiQueue.put(runnable)
    }

    override fun readKey(): Key {
        val producer = keyStrokeProducer!!
        while (true) {
            val key = producer.readKey() // must be initialized by now

            // TODO check for esc/ctrl+c and throw InputInterruptedException...
            // TODO catch that in the feedKey loop

            if (key == null) {
                // check the onMainThread-to-UI-thread queue
                // run a chunk at a time
                for (i in 0..8) {
                    val runnable = postToUiQueue.poll()
                    if (runnable != null) runnable.invoke()
                    else break
                }
            } else {
                if (key.keyCode !in UNRECORDED_KEY_CODES) {
                    undo.onKeyStroke(key)
                }
                return key
            }
        }
    }

    override fun searchForKeyword(text: CharSequence, direction: Int) {
        renderer.inTransaction {
            tabpage.currentWindow.searchForKeyword(text, direction)
        }
    }

    override fun setCursorType(type: CursorType) =
        renderer.setCursorType(type)

    override fun quit() {
        connection?.close()
        renderer.close()
        running = false
    }

    override fun unmap(mode: String, from: String) {
        modes[mode]?.let { modeObj ->
            if (modeObj !is MappableMode) {
                throw IllegalArgumentException("$mode does not support mapping")
            }

            val fromKeys = Keys.parse(from)
            modeObj.userMappings.unmap(fromKeys)
            return
        }

        // map in all modes
        if (mode == "") {
            modes.keys
                .filter { modes[it] is MappableMode }
                .forEach { unmap(it, from) }
            return
        }

        throw IllegalArgumentException("No such mode $mode")
    }

    @Synchronized internal fun onDisconnect(connection: JudoConnection) {
        doEcho = true
        renderer.inTransaction {
            // dump the parsed prompts for visual effect
            echo("")
            parsedPrompts.forEach {
                primaryWindow.outputWindow.appendLine(it, isPartialLine = false)
            }
            parsedPrompts.clear()
            primaryWindow.promptBuffer.clear()
            tabpage.unsplit()

            echo("Disconnected from $connection")
            updateStatusLine(currentMode)

            // stop logging
            logging.unconfigure()

            onMainThread {
                events.raise("DISCONNECTED")
                events.clear()

                // don't clear mapper until after we've fired
                // any disconnect events
                mapper.clear()
            }
        }

        this.connection = null

        state[KEY_PERSIST_INPUT_HISTORY_PATH]?.let { path ->
            redirectErrors("FAILED TO PERSIST INPUT: ") {
                sendHistory.writeTo(path)
            }
        }
        state.remove(KEY_PERSIST_INPUT_HISTORY_PATH)
    }

    /**
     * Read a file in command mode
     */
    fun readFile(file: File) {
        val cmdMode = modes["cmd"] as BaseCmdMode

        redirectErrors("ERROR: ") {
            cmdMode.readFile(file)
        }
    }

    /**
     * Execute some script in command mode;
     * this is for testing purposes
     */
    internal fun executeScript(code: String) {
        val cmdMode = modes["cmd"] as BaseCmdMode
        cmdMode.execute(code)
    }

    /**
     * Read keys forever from the given producer
     */
    fun readKeys(producer: BlockingKeySource) {
        readKeys(producer, remap = true, fromMap = false) { running }
    }

    /**
     * Read keys from the given producer until [keepReading] returns false
     */
    fun readKeys(producer: BlockingKeySource, remap: Boolean, fromMap: Boolean, keepReading: () -> Boolean) {
        val oldProducer = keyStrokeProducer

        keyStrokeProducer = producer
        while (keepReading()) {
            redirectErrors("INTERNAL ERROR: ") {
                feedKey(readKey(), remap, fromMap)
            }

            Thread.yield()
        }

        if (oldProducer != null) {
            keyStrokeProducer = oldProducer
        }
    }

    internal fun processOutput(line: OutputLine) {
        logging.log(line)

        // convert to AttributedString before processing
        // so we can ignore ANSI stuff when matching
        val attributed = line.toAttributedString()
        outputCompletions.process(attributed)
        triggers.process(attributed)
        processAndStripPrompt(attributed)
    }

    private fun activateMode(mode: Mode) {
        renderer.inTransaction {
            renderer.setCursorType(CursorType.BLOCK)

            currentMode.onExit()
            currentMode = mode
            mode.onEnter()

            updateInputLine()

            if (mode is StatusBufferProvider) {
                tabpage.currentWindow.updateStatusLine(mode.renderStatusBuffer(), mode.getCursor())
            } else {
                updateStatusLine(mode)
            }
        }
    }

    private fun updateInputLine() {
        if (!doEcho) {
            renderer.updateInputLine("", 0)
            return
        }

        val mode = currentMode
        if (mode is InputBufferProvider) {
            renderer.updateInputLine(mode.renderInputBuffer(), mode.getCursor())
        } else {
            renderer.updateInputLine(buffer.toString(), buffer.cursor)
        }
    }

    private fun updateStatusLine(mode: Mode) {
        tabpage.currentWindow.updateStatusLine(buildStatusLine(mode).toAnsiString())
    }

    internal fun buildStatusLine(mode: Mode): IStringBuilder {
        val modeIndicator = "[${mode.name.toUpperCase()}]"
        val availableCols = renderer.windowWidth - modeIndicator.length
        statusLineWorkspace.setLength(0)
        if (parsedPrompts.isNotEmpty()) {
            val prompt = parsedPrompts.last()
            val promptColsToPrint = minOf(prompt.length, availableCols)
            val partialPrompt = prompt.subSequence(0, promptColsToPrint)
            statusLineWorkspace.append(partialPrompt)
        }

        for (i in statusLineWorkspace.length..availableCols-1) {
            statusLineWorkspace.append(" ")
        }

        statusLineWorkspace.append(modeIndicator)
        return statusLineWorkspace
    }

    @Synchronized
    private fun appendError(e: Throwable, prefix: String = "", isRoot: Boolean = true) {
        if (isRoot) {
            PrintWriter(FileOutputStream(debugLogFile, true)).use {
                it.println(prefix)
                e.printStackTrace(it)
            }
        }

        if (e is ScriptExecutionException) {
            renderer.inTransaction {
                appendOutput(OutputLine("${prefix}ScriptExecutionException:\n${e.message}\n"), process = false)
                e.stackTrace.map { "  $it" }
                    .forEach { primaryWindow.appendLine(it, isPartialLine = false) }
            }
            return
        }

        renderer.inTransaction {
            appendOutput(OutputLine("$prefix${e.javaClass.name}: ${e.message}\n"), process = false)
            e.stackTrace.map { "  $it" }
                .forEach { primaryWindow.appendLine(it, isPartialLine = false) }
            e.cause?.let {
                appendError(it, "Caused by: ", isRoot = false)
            }
        }
    }

    /** NOTE: public for testing only */
    fun onIncomingBuffer(buffer: CharArray, count: Int) {
        renderer.inTransaction {
            val line = OutputLine(buffer, 0, count)

            redirectErrors("ERROR: ") {
                appendOutput(line)
            }
        }
    }

    @Synchronized internal fun appendOutput(buffer: OutputLine, process: Boolean = true) {
        val count = buffer.length
        renderer.inTransaction {
            var lastLineEnd = 0

            for (i in 0 until count) {
                if (i >= lastLineEnd) {
                    val char = buffer[i]
                    if (!(char == '\n' || char == '\r')) continue

                    val opposite =
                        if (char == '\n') '\r'
                        else '\n'

                    val actualLine = primaryWindow.appendLine(
                        buffer.subSequence(lastLineEnd, i),
                        isPartialLine = false
                    ) as OutputLine
                    if (process) processOutput(actualLine)

                    if (i + 1 < count && buffer[i + 1] == opposite) {
                        lastLineEnd = i + 2
                    } else {
                        lastLineEnd = i + 1
                    }
                }
            }

            if (lastLineEnd < count) {
                primaryWindow.appendLine(
                    buffer.subSequence(lastLineEnd, count),
                    isPartialLine = true
                )
            }
        }
    }

    private fun processAndStripPrompt(actualLine: CharSequence) {
        val originalLength = actualLine.length
        val result = prompts.process(actualLine, this::onPrompt)
        if (result.length != originalLength) {
            renderer.inTransaction {
                // we found a prompt! clean up the output
                outputBuffer.replaceLastLine(result)
            }
        }
    }

    internal fun onPrompt(index: Int, prompt: CharSequence) {
        if (parsedPrompts.lastIndex < index) {
            for (i in maxOf(0, parsedPrompts.lastIndex)..index) {
                parsedPrompts.add(IStringBuilder.EMPTY)
            }
        }

        // make sure prompt colors don't bleed
        parsedPrompts[index] = IStringBuilder.from(prompt)

        renderer.inTransaction {
            primaryWindow.setPromptHeight(parsedPrompts.size)
            primaryWindow.promptBuffer.set(parsedPrompts)
            updateStatusLine(currentMode)
        }
    }

    private inline fun redirectErrors(prefix: String = "", block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            appendError(e, prefix)
        }
    }
}

