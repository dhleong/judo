package net.dhleong.judo

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
import net.dhleong.judo.input.action
import net.dhleong.judo.input.changes.UndoManager
import net.dhleong.judo.logging.LogManager
import net.dhleong.judo.mapping.MapManager
import net.dhleong.judo.mapping.MapRenderer
import net.dhleong.judo.modes.BaseCmdMode
import net.dhleong.judo.modes.BlockingEchoMode
import net.dhleong.judo.modes.CmdMode
import net.dhleong.judo.modes.InputBufferProvider
import net.dhleong.judo.modes.InsertMode
import net.dhleong.judo.modes.MappableMode
import net.dhleong.judo.modes.NormalMode
import net.dhleong.judo.modes.OperatorPendingMode
import net.dhleong.judo.modes.OutputSearchMode
import net.dhleong.judo.modes.ReverseInputSearchMode
import net.dhleong.judo.modes.ScriptExecutionException
import net.dhleong.judo.modes.StatusBufferProvider
import net.dhleong.judo.modes.UserCreatedMode
import net.dhleong.judo.net.JudoConnection
import net.dhleong.judo.net.isTelnetSubsequence
import net.dhleong.judo.prompt.AUTO_UNIQUE_GROUP_ID
import net.dhleong.judo.prompt.PromptManager
import net.dhleong.judo.register.RegisterManager
import net.dhleong.judo.render.Flavor
import net.dhleong.judo.render.FlavorableCharSequence
import net.dhleong.judo.render.FlavorableStringBuilder
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.PrimaryJudoWindow
import net.dhleong.judo.render.parseAnsi
import net.dhleong.judo.render.toFlavorable
import net.dhleong.judo.script.JythonScriptingEngine
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.trigger.TriggerManager
import net.dhleong.judo.util.InputHistory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import java.io.PrintWriter
import java.net.URI
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
    userConfigDir: File = File(".judo"),
    userConfigFile: File = File(userConfigDir, "init.py"),
    scripting: ScriptingEngine.Factory = JythonScriptingEngine.Factory(),
    val debug: DebugLevel = DebugLevel.OFF,
    private val connections: JudoConnection.Factory
) : IJudoCore {
    companion object {
        const val CLIENT_NAME = BuildConfig.NAME
        const val CLIENT_VERSION = BuildConfig.VERSION
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

    private val parsedPrompts = ArrayList<FlavorableCharSequence>(2)
    private var lastPromptGroup = AUTO_UNIQUE_GROUP_ID

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
        CmdMode(
            this, ids, cmdBuffer, renderer, cmdHistory, completions,
            userConfigDir,
            userConfigFile,
            scripting
        ),
        ReverseInputSearchMode(this, buffer, sendHistory)

    ).fold(HashMap<String, Mode>()) { map, mode ->
        map[mode.name] = mode
        map
    }

    // internal for testing
    internal var currentMode: Mode = normalMode
    private val modeStack = ArrayList<Mode>()

    internal var running = true
    internal var doEcho = true

    override var connection: JudoConnection? = null
    private var lastConnect: URI? = null

    private val statusLineWorkspace = FlavorableStringBuilder(128)

    private var keyStrokeProducer: BlockingKeySource? = null
    private val postToUiQueue = ArrayBlockingQueue<() -> Unit>(64)

    private val outputBuffer: IJudoBuffer
    private val primaryWindow: PrimaryJudoWindow
    override val tabpage: IJudoTabpage = renderer.currentTabpage

    init {
        primaryWindow = tabpage.currentWindow as PrimaryJudoWindow
        outputBuffer = primaryWindow.outputWindow.currentBuffer

        primaryWindow.isFocused = true
        renderer.currentTabpage = tabpage
        renderer.settings = state
        renderer.onEvent = { ev -> when (ev) {
            JudoRendererEvent.OnLayout,
            JudoRendererEvent.OnResized -> onResize()
            JudoRendererEvent.OnBlockingEcho -> onBlockingEcho()
        } }

        if (debug.isEnabled) {
            System.setErr(PrintStream(debugLogFile.outputStream()))
        }

        activateMode(currentMode)
    }

    private fun onResize() = renderer.inTransaction {
        updateStatusLine(currentMode)
        updateInputLine()

        tabpage.currentWindow.let {
            mapper.resize(width = it.width)
        }
    }

    private fun onBlockingEcho() {
        enterMode(BlockingEchoMode(this, renderer))
    }

    override fun connect(uri: URI) {
        disconnect()
        doPrint(false, "Connecting to $uri... ")

        lastConnect = uri

        val connection: JudoConnection
        try {
            connection = connections.create(this, uri)
                ?: return doPrint(false,
                    "Don't know how to connect to $uri"
                )
            print("Connected.")
        } catch (e: IOException) {
            appendError(e, "Failed.\nNETWORK ERROR: ")
            return
        }

        connection.setWindowSize(renderer.windowWidth, renderer.windowHeight)
        connection.onDisconnect = this::onDisconnect
        connection.onEchoStateChanged = { doEcho ->
            this.doEcho = doEcho

            if (debug.isEnabled) {
                print("## TELNET doPrint($doEcho)")
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
        connection.forEachLine { buffer ->
            onIncomingBuffer(buffer)
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
        renderer.echo(objects.toFlavoredSequence())
    }

    override fun print(vararg objects: Any?) = doPrint(true, objects.toFlavoredSequence())
    override fun printRaw(vararg objects: Any?) = doPrint(false, objects.toFlavoredSequence())

    private fun doPrint(process: Boolean, asString: String) =
        doPrint(process, FlavorableStringBuilder.withDefaultFlavor(asString))
    private fun doPrint(process: Boolean, flavorable: FlavorableCharSequence) {
        appendOutput(when {
            flavorable.endsWith('\n') -> flavorable
            else -> flavorable + "\n"
        }, process = process)

        if (debug.isEnabled) {
            debugLogFile.appendText("\n## ECHO: $flavorable\n")
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
            modeObj.userMappings.map(fromKeys, action(to), description)
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
        // TODO if mode.isBlank() print ALL mappings
        modes[mode]?.let { modeObj ->
            if (modeObj !is MappableMode) {
                throw IllegalArgumentException("$mode does not support mapping")
            }

            printRaw(modeObj.userMappings)
            return
        }

        throw IllegalArgumentException("No such mode $mode")
    }

    override fun reconnect() {
        lastConnect?.let { uri ->
            connect(uri)
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

        if (doEcho && !isTelnetSubsequence(toSend)) {
            // always output what we sent
            // except... don't print if the server has told us not to
            // (EG: passwords)
            print(toSend) // TODO color?
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

            connection?.let { conn ->
                GlobalScope.launch {
                    conn.send(toSend)
                }
                return
            }

            appendError(Error("Not connected."))
        }
    }

    override fun feedKey(stroke: Key, remap: Boolean, fromMap: Boolean) = renderer.inTransaction {
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
        if (newMode is StatusBufferProvider) {
            tabpage.currentWindow.updateStatusLine(
                newMode.renderStatusBuffer(),
                newMode.getCursor()
            )
        } else {
            updateInputLine()
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
            print("")
            parsedPrompts.forEach {
                primaryWindow.outputWindow.appendLine(it)
            }
            parsedPrompts.clear()
            primaryWindow.promptWindow.currentBuffer.clear()
            tabpage.unsplit()

            print("Disconnected from $connection")
            updateStatusLine(currentMode)

            // stop logging
            logging.unconfigure()

            // ensure the connection gets a chance to clean up
            try {
                connection.close()
            } catch (e: IOException) {
                // ignore
            }

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

    internal fun processOutput(line: FlavorableCharSequence) {
        outputCompletions.process(line)
        triggers.process(line)
        processAndStripPrompt(line)
    }

    private fun activateMode(mode: Mode) {
        renderer.inTransaction {
            renderer.setCursorType(CursorType.BLOCK)

            currentMode.onExit()
            currentMode = mode
            mode.onEnter()

            updateInputLine()

            if (mode is StatusBufferProvider) {
                tabpage.currentWindow.updateStatusLine(
                    mode.renderStatusBuffer(),
                    mode.getCursor()
                )
            } else {
                updateStatusLine(mode)
            }
        }
    }

    private fun updateInputLine() = renderer.inTransaction {
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
        tabpage.currentWindow.apply {
            updateStatusLine(buildStatusLine(this, mode))
        }
    }

    internal fun buildStatusLine(window: IJudoWindow, mode: Mode): FlavorableCharSequence {
        val modeIndicator = "[${mode.name.toUpperCase()}]"
        val availableCols = window.width - modeIndicator.length
        statusLineWorkspace.setLength(0)
        if (parsedPrompts.isNotEmpty()) {
            val prompt = parsedPrompts.last()
            val promptColsToPrint = minOf(prompt.length, availableCols)
            val partialPrompt = prompt.subSequence(0, promptColsToPrint).apply {
                removeTrailingNewline()
            }
            statusLineWorkspace.append(partialPrompt)
        }

        for (i in statusLineWorkspace.length until availableCols) {
            statusLineWorkspace += " "
        }

        statusLineWorkspace.append(modeIndicator, Flavor.default)
        return statusLineWorkspace.toFlavorableString()
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
                primaryWindow.appendLine(
                    "${prefix}ScriptExecutionException:\n"
                )
                e.message?.split("\n")?.forEach {
                    primaryWindow.appendLine(it)
                }
                e.stackTrace.map { "  $it" }.forEach {
                    primaryWindow.appendLine(it)
                }
            }
            return
        }

        renderer.inTransaction {
            primaryWindow.appendLine(
                "$prefix${e.javaClass.name}: ${e.message}"
            )
            e.stackTrace.map { "  $it" }.forEach {
                primaryWindow.appendLine(it)
            }
            e.cause?.let {
                appendError(it, "Caused by: ", isRoot = false)
            }
        }
    }

    /** NOTE: public for testing only */
    fun onIncomingBuffer(line: FlavorableCharSequence) = renderer.inTransaction {
        redirectErrors("ERROR: ") {
            appendOutput(line)
        }
    }

    @Synchronized internal fun appendOutput(
        output: FlavorableCharSequence,
        process: Boolean = true
    ) {
        val buffer = primaryWindow.currentBuffer
        primaryWindow.append(output)
        if (process) {
            val actualLine = buffer[buffer.lastIndex]
            processOutput(actualLine)
        }
    }

    private fun processAndStripPrompt(actualLine: FlavorableCharSequence) {
        val originalLength = actualLine.length
        val result = prompts.process(actualLine, this::onPrompt)
        if (result.length != originalLength) {
            renderer.inTransaction {
                // we found a prompt! clean up the output
                outputBuffer.replaceLastLine(result.toFlavorable())
            }
        }

        if (result.endsWith('\n')) {
            // only log lines that have a newline
            logging.log(result)
        }
    }

    internal fun onPrompt(group: Int, prompt: String, index: Int) {
        if (group != lastPromptGroup) {
            lastPromptGroup = group
            parsedPrompts.clear()
        }
        if (parsedPrompts.lastIndex < index) {
            val neededRows = index - parsedPrompts.lastIndex
            for (i in 0 until neededRows) {
                parsedPrompts.add(FlavorableStringBuilder.EMPTY)
            }
        }

        parsedPrompts[index] = prompt.parseAnsi()

        renderer.inTransaction {
            primaryWindow.setPromptHeight(parsedPrompts.size)
            primaryWindow.promptWindow.currentBuffer.set(parsedPrompts)
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

