package net.dhleong.judo

import net.dhleong.judo.alias.AliasManager
import net.dhleong.judo.alias.AliasProcessingException
import net.dhleong.judo.complete.CompletionSourceFacade
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Keys
import net.dhleong.judo.modes.BaseCmdMode
import net.dhleong.judo.modes.InsertMode
import net.dhleong.judo.modes.MappableMode
import net.dhleong.judo.modes.NormalMode
import net.dhleong.judo.modes.OperatorPendingMode
import net.dhleong.judo.modes.PythonCmdMode
import net.dhleong.judo.modes.ReverseInputSearchMode
import net.dhleong.judo.net.CommonsNetConnection
import net.dhleong.judo.net.Connection
import net.dhleong.judo.prompt.PromptManager
import net.dhleong.judo.trigger.TriggerManager
import net.dhleong.judo.util.IStringBuilder
import net.dhleong.judo.util.InputHistory
import net.dhleong.judo.util.stripAnsi
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

class JudoCore(val renderer: JudoRenderer) : IJudoCore {

    override val aliases = AliasManager()
    override val triggers = TriggerManager()
    override val prompts = PromptManager()
    override var opfunc: OperatorFunc? = null

    private val parsedPrompts = ArrayList<IStringBuilder>(1)

    internal val buffer = InputBuffer()
    private val sendHistory = InputHistory(buffer)
    private val completions = CompletionSourceFacade.create()

    private val opMode = OperatorPendingMode(this, buffer)
    private val normalMode = NormalMode(this, buffer, sendHistory, opMode)

    private val modes = sequenceOf(

        InsertMode(this, buffer, completions),
        normalMode,
        opMode,
        PythonCmdMode(this),
        ReverseInputSearchMode(this, buffer, sendHistory)

    ).fold(HashMap<String, Mode>(), { map, mode ->
        map[mode.name] = mode
        map
    })

    private var currentMode: Mode = normalMode

    internal var running = true

    private var connection: Connection? = null

    private val statusLineWorkspace = IStringBuilder.create(128)

    private lateinit var keyStrokeProducer: BlockingKeySource

    init {
        activateMode(currentMode)
        renderer.onResized = {
            updateStatusLine(currentMode)
            updateInputLine()
        }
    }

    override fun connect(address: String, port: Int) {
        disconnect()
        echo("Connecting to $address:$port...")

//        val logFile = File("log.txt")

        val connection = CommonsNetConnection(address, port, renderer.terminalType)
        connection.setWindowSize(renderer.windowWidth, renderer.windowHeight)
        connection.onDisconnect = this::onDisconnect
        connection.onError = { appendError(it, "NETWORK ERROR: ")}
        connection.forEachLine { buffer, count ->
//            logFile.appendText(String(buffer, 0, count))
//            logFile.appendText("{PACKET_BREAK}")
            renderer.inTransaction {
                try {
                    val asCharSequence = StringBuilder(count).append(buffer, 0, count)
                    val withoutPrompts = prompts.process(asCharSequence, this::onPrompt)
                    appendOutput(withoutPrompts)
                    processOutput(stripAnsi(withoutPrompts))
                } catch (e: Throwable) {
                    appendError(e, "ERROR")
                }
            }
        }

        this.connection = connection
    }

    override fun disconnect() {
        connection?.let {
            it.onDisconnect = null
            it.onError = null
            it.close()
            connection = null
            echo("Disconnected from $it.")
        }
    }

    override fun echo(vararg objects: Any?) {
        // TODO colors?
        renderer.appendOutput(objects.joinToString(" "))
    }

    override fun enterMode(modeName: String) {
        val mode = modes[modeName]
        if (mode != null) {
            activateMode(mode)
        } else {
            throw IllegalArgumentException("No such mode `$modeName`")
        }
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

    override fun scrollPages(count: Int) {
        renderer.scrollPages(count)
    }

    override fun scrollToBottom() {
        renderer.scrollToBottom()
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

        // always output what we sent
        // TODO except... don't echo if the server has told us not to
        // (EG: passwords)
        echo(toSend) // TODO color?

        if (!fromMap && !toSend.isEmpty()) {
            // record it even if we couldn't send it
            sendHistory.push(toSend)
            sendHistory.resetHistoryOffset() // start back from most recent

            // also complete from sent things
            // (but the original text, not the alias-processed one)
            completions.process(text)
        }

        connection?.let {
            it.send(toSend)
            return
        }

        appendError(Error("Not connected."))
    }

    override fun feedKey(stroke: KeyStroke, remap: Boolean) {
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

        currentMode.feedKey(stroke, remap)

        // NOTE: currentMode might have changed as a result of feedKey
        val newMode = currentMode
        if (newMode is BaseCmdMode) {
            renderer.updateStatusLine(":${newMode.inputBuffer}", newMode.inputBuffer.cursor + 1)
        } else {
            updateInputLine()
        }
    }

    // TODO check for esc/ctrl+c and throw InputInterruptedException...
    // TODO catch that in the feedKey loop
    override fun readKey(): KeyStroke =
        keyStrokeProducer.readKey()

    override fun quit() {
        connection?.close()
        renderer.close()
        running = false
    }

    fun onDisconnect() {
        renderer.inTransaction {
            // dump the parsed prompts for visual affect
            parsedPrompts.forEach {
                renderer.appendOutput(it)
            }
            parsedPrompts.clear()

            echo("Disconnected from $connection")
            updateStatusLine(currentMode)
        }
    }

    /**
     * Read a file in command mode
     */
    fun readFile(file: File) {
        val cmdMode = modes["cmd"] as BaseCmdMode

        try {
            file.inputStream().use {
                cmdMode.readFile(file.name, it)
            }
        } catch (e: Throwable) {
            appendError(e, "ERROR: ")
        }
    }

    /**
     * Read keys forever from the given producer
     */
    fun readKeys(producer: BlockingKeySource) {
        keyStrokeProducer = producer
        while (running) {
            try {
                feedKey(producer.readKey(), true)
            } catch (e: Throwable) {
                appendError(e, "INTERNAL ERROR: ")
            }

            Thread.yield()
        }
    }

    fun processOutput(rawOutput: CharSequence) {
        completions.process(rawOutput)
        triggers.process(rawOutput)
    }

    private fun activateMode(mode: Mode) {
        renderer.inTransaction {
            currentMode = mode
            mode.onEnter()

            updateInputLine()

            if (mode is BaseCmdMode) {
                renderer.updateStatusLine(":", 1)
            } else {
                updateStatusLine(mode)
            }
        }
    }

    private fun updateInputLine() {
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

    private fun appendError(e: Throwable, prefix: String = "") {
        renderer.appendOutput("$prefix${e.message}")
        e.stackTrace.map { "  $it" }
            .forEach { renderer.appendOutput(it) }
        e.cause?.let {
            appendError(it, "Caused by: ")
        }
    }

    internal fun appendOutput(buffer: CharSequence) {
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

                    renderer.appendOutput(
                        buffer.subSequence(lastLineEnd, i),
                        isPartialLine = false
                    )

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

    internal fun onPrompt(index: Int, prompt: CharSequence) {
        if (parsedPrompts.lastIndex < index) {
            parsedPrompts.addAll((parsedPrompts.lastIndex..index).map { IStringBuilder.EMPTY })
        }

        parsedPrompts[index] = IStringBuilder.from(prompt)
        updateStatusLine(currentMode)
    }
}

