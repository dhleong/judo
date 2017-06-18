package net.dhleong.judo

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.alias.AliasProcessingException
import net.dhleong.judo.complete.MultiplexCompletionSource
import net.dhleong.judo.complete.RecencyCompletionSource
import net.dhleong.judo.complete.multiplex.WeightedRandomSelector
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Keys
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
import net.dhleong.judo.prompt.PromptManager
import net.dhleong.judo.render.OutputLine
import net.dhleong.judo.trigger.TriggerManager
import net.dhleong.judo.util.IStringBuilder
import net.dhleong.judo.util.InputHistory
import net.dhleong.judo.util.ansi
import java.awt.event.KeyEvent
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import javax.swing.KeyStroke

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

class JudoCore(
    val renderer: JudoRenderer,
    val debug: DebugLevel = DebugLevel.OFF
) : IJudoCore {

    override val aliases = AliasManager()
    override val triggers = TriggerManager()
    override val prompts = PromptManager()
    override val state = StateMap()

    private val parsedPrompts = ArrayList<IStringBuilder>(2)

    internal val buffer = InputBuffer()
    internal val cmdBuffer = InputBuffer()

    private val sendHistory = InputHistory(buffer)
    private val cmdHistory = InputHistory(cmdBuffer)

    private val commandCompletions = RecencyCompletionSource()
    private val outputCompletions = RecencyCompletionSource()
    private val weightedSelectorFactory = WeightedRandomSelector.distributeByWordIndex(
        // first word? prefer commandCompletions by a lot;
        // we'll still fallback to output if commandCompletion
        // doesn't have anything
        doubleArrayOf(.95, .05),

        // second word? slight preference to commands
        doubleArrayOf(.65, .35),

        // otherwise, just split it evenly
        doubleArrayOf(.5, .5)
    )
    private val completions = MultiplexCompletionSource(
        listOf(commandCompletions, outputCompletions),
        { string, wordRange ->

            val wordsBefore =
                // convenient shortcut
                if (wordRange.start == 0) 0

                // TODO: optimize and fix (double whitespace anyone?)
                else string.subSequence(0, wordRange.start)
                    .count { Character.isWhitespace(it) }

            weightedSelectorFactory(wordsBefore)
        }
    )

    private val opMode = OperatorPendingMode(this, buffer)
    private val normalMode = NormalMode(this, buffer, sendHistory, opMode)

    private val debugLogFile = File("debug-log.txt")

    private val modes = sequenceOf(

        InsertMode(this, buffer, completions, sendHistory),
        normalMode,
        opMode,
        OutputSearchMode(this),
        PythonCmdMode(this, cmdBuffer, renderer, cmdHistory, completions),
        ReverseInputSearchMode(this, buffer, sendHistory)

    ).fold(HashMap<String, Mode>(), { map, mode ->
        map[mode.name] = mode
        map
    })

    private var currentMode: Mode = normalMode

    internal var running = true
    internal var doEcho = true

    override var connection: Connection? = null
    private var lastConnect: Pair<String, Int>? = null

    private val statusLineWorkspace = IStringBuilder.create(128)

    private var keyStrokeProducer: BlockingKeySource? = null

    init {
        activateMode(currentMode)
        renderer.onResized = {
            updateStatusLine(currentMode)
            updateInputLine()
        }

        if (debug.isEnabled) {
            System.setErr(PrintStream(debugLogFile.outputStream()))
        }
    }

    override fun connect(address: String, port: Int) {
        disconnect()
        echo("Connecting to $address:$port...")

        lastConnect = address to port

        val connection = CommonsNetConnection(address, port, renderer, { string -> echo(string) })
        connection.debug = debug.isEnabled
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
        // TODO colors?
        val asString = objects.joinToString(" ")
        renderer.appendOutput("${ansi(0)}$asString")

        if (debug.isEnabled) {
            debugLogFile.appendText("\n## ECHO: $asString\n")
        }
    }

    override fun enterMode(modeName: String) {
        val mode = modes[modeName]
        if (mode != null) {
            activateMode(mode)
        } else {
            throw IllegalArgumentException("No such mode `$modeName`")
        }
    }

    override fun enterMode(mode: Mode) {
        activateMode(mode)
    }

    override fun exitMode() {
        // TODO actually, return to the previous mode
        activateMode(normalMode)
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

    override fun map(mode: String, from: String, to: () -> Unit) {
        modes[mode]?.let { modeObj ->
            if (modeObj !is MappableMode) {
                throw IllegalArgumentException("$mode does not support mapping")
            }

            val fromKeys = Keys.parse(from)
            modeObj.userMappings.map(fromKeys, { _ -> to() })
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

    override fun reconnect() {
        lastConnect?.let { (address, port) ->
            connect(address, port)
            return
        }

        throw IllegalStateException("You have to connect() first")
    }

    override fun scrollPages(count: Int) {
        renderer.scrollPages(count)
    }

    override fun scrollToBottom() {
        renderer.scrollToBottom()
    }

    override fun seedCompletion(text: String) {
        commandCompletions.process(text)
    }

    override fun send(text: String, fromMap: Boolean) {
        scrollToBottom()

        val toSend: String
        try {
            toSend = aliases.process(text).toString()
        } catch (e: AliasProcessingException) {
            appendError(e)
            return
        }

        if (doEcho) {
            // always output what we sent
            // except... don't echo if the server has told us not to
            // (EG: passwords)
            echo(toSend) // TODO color?
        }

        if (!fromMap && !toSend.isEmpty()) {
            // record it even if we couldn't send it
            sendHistory.push(toSend)
            sendHistory.resetHistoryOffset() // start back from most recent

            // also complete from sent things
            // (but the original text, not the alias-processed one)
            completions.process(text) // NOTE: we let all completers process commands
        }

        connection?.let {
            it.send(toSend)
            return
        }

        appendError(Error("Not connected."))
    }

    override fun feedKey(stroke: KeyStroke, remap: Boolean, fromMap: Boolean) {
//        echo("## feedKey($stroke)")
        when (stroke.keyCode) {
            KeyEvent.VK_ESCAPE -> {
                if (currentMode != normalMode) {
                    activateMode(normalMode)
                }
                return
            }

            KeyEvent.VK_PAGE_UP -> {
                renderer.scrollLines(1)
                return
            }
            KeyEvent.VK_PAGE_DOWN -> {
                renderer.scrollLines(-1)
                return
            }
        }

        currentMode.feedKey(stroke, remap, fromMap)

        // NOTE: currentMode might have changed as a result of feedKey
        val newMode = currentMode
        if (newMode is StatusBufferProvider) {
            renderer.updateStatusLine(newMode.renderStatusBuffer(), newMode.getCursor())
        } else {
            updateInputLine()
        }
    }

    override fun feedKeys(keys: String, remap: Boolean, mode: String) {
        val oldMode = currentMode

        if (mode != "") {
            // NOTE: we don't activateMode() because there's a lot of baggage
            // associated with that, and I don't think Vim does that stuff either
            val newMode = modes[mode] ?: throw IllegalArgumentException("No such mode `$mode`")
            currentMode = newMode
        }

        val keysIter = Keys.parse(keys).iterator()
        readKeys(object : BlockingKeySource {
            override fun readKey(): KeyStroke = keysIter.next()
        }, remap = remap, fromMap = true) {
            keysIter.hasNext()
        }

        if (mode != "") {
            // restore the old mode (but without calling onEnter and making it
            // reset its state)
            currentMode = oldMode
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

    override fun readKey(): KeyStroke {
        val key = keyStrokeProducer!!.readKey() // must be initialized by now
        // TODO check for esc/ctrl+c and throw InputInterruptedException...
        // TODO catch that in the feedKey loop
        return key
    }

    override fun searchForKeyword(text: CharSequence, direction: Int) {
        renderer.searchForKeyword(text, direction)
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

        throw IllegalArgumentException("No such mode $mode")    }

    fun onDisconnect(connection: Connection) {
        renderer.inTransaction {
            // dump the parsed prompts for visual effect
            echo("")
            parsedPrompts.forEach {
                renderer.appendOutput(it)
            }
            parsedPrompts.clear()

            echo("Disconnected from $connection")
            updateStatusLine(currentMode)
        }

        this.connection = null

        state[KEY_PERSIST_INPUT_HISTORY_PATH]?.let { path ->
            try {
                sendHistory.writeTo(path)
            } catch (e: Exception) {
                appendError(e, "FAILED TO PERSIST INPUT: ")
            }
        }
        state.remove(KEY_PERSIST_INPUT_HISTORY_PATH)
    }

    /**
     * Read a file in command mode
     */
    fun readFile(file: File) {
        val cmdMode = modes["cmd"] as BaseCmdMode

        try {
            cmdMode.readFile(file)
        } catch (e: Throwable) {
            appendError(e, "ERROR: ")
        }
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
            try {
                feedKey(producer.readKey(), remap, fromMap)
            } catch (e: Throwable) {
                appendError(e, "INTERNAL ERROR: ")
            }

            Thread.yield()
        }

        if (oldProducer != null) {
            keyStrokeProducer = oldProducer
        }
    }

    fun processOutput(rawOutput: CharSequence) {
        outputCompletions.process(rawOutput)
        triggers.process(rawOutput)
        processAndStripPrompt(rawOutput)
    }

    private fun activateMode(mode: Mode) {
        renderer.inTransaction {
            renderer.setCursorType(CursorType.BLOCK)

            currentMode.onExit()
            currentMode = mode
            mode.onEnter()

            updateInputLine()

            if (mode is StatusBufferProvider) {
                renderer.updateStatusLine(mode.renderStatusBuffer(), mode.getCursor())
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
        renderer.updateStatusLine(buildStatusLine(mode).toAnsiString())
    }

    internal fun buildStatusLine(mode: Mode): IStringBuilder {
        val modeIndicator = "[${mode.name.toUpperCase()}]"
        val availableCols = renderer.windowWidth - modeIndicator.length
        statusLineWorkspace.setLength(0)
        if (parsedPrompts.isNotEmpty()) {
            // TODO support multiple prompts?
            val prompt = parsedPrompts[0]
            val promptColsToPrint = minOf(prompt.length, availableCols)
            val partialPrompt = prompt.subSequence(0, promptColsToPrint)
            statusLineWorkspace.append(partialPrompt)
        }

        for (i in statusLineWorkspace.length until availableCols) {
            statusLineWorkspace.append(" ")
        }

        statusLineWorkspace.append(modeIndicator)
        return statusLineWorkspace
    }

    private fun appendError(e: Throwable, prefix: String = "", isRoot: Boolean = true) {
        if (isRoot) {
            debugLogFile.printWriter().use {
                it.println(prefix)
                e.printStackTrace(it)
            }
        }

        if (e is ScriptExecutionException) {
            renderer.appendOutput("${prefix}ScriptExecutionException:")
            appendOutput(OutputLine(e.message!!))
            e.stackTrace.map { "  $it" }
                .forEach { renderer.appendOutput(it) }
            return
        }

        renderer.appendOutput("$prefix${e.javaClass.name}: ${e.message} ")
        e.stackTrace.map { "  $it" }
            .forEach { renderer.appendOutput(it) }
        e.cause?.let {
            appendError(it, "Caused by: ", isRoot = false)
        }
    }

    /** NOTE: public for testing only */
    fun onIncomingBuffer(buffer: CharArray, count: Int) {
        renderer.inTransaction {
            try {
                val line = OutputLine(buffer, 0, count)
                appendOutput(line)
            } catch (e: Throwable) {
//                appendError(e, "ERROR")
                throw e
            }
        }
    }

    internal fun appendOutput(buffer: OutputLine) {
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

                    val actualLine = renderer.appendOutput(
                        buffer.subSequence(lastLineEnd, i),
                        isPartialLine = false
                    ) as OutputLine
                    processOutput(actualLine.toAttributedString())

                    if (i + 1 < count && buffer[i + 1] == opposite) {
                        lastLineEnd = i + 2
                    } else {
                        lastLineEnd = i + 1
                    }
                }
            }

            if (lastLineEnd < count) {
                renderer.appendOutput(
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
            // we found a prompt! clean up the output
            renderer.replaceLastLine(result)
        }
    }

    internal fun onPrompt(index: Int, prompt: CharSequence) {
        if (parsedPrompts.lastIndex < index) {
            parsedPrompts.addAll((parsedPrompts.lastIndex..index).map { IStringBuilder.EMPTY })
        }

        // make sure prompt colors don't bleed
        parsedPrompts[index] = IStringBuilder.from(prompt)
        updateStatusLine(currentMode)
    }
}

