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
import net.dhleong.judo.modes.PythonCmdMode
import net.dhleong.judo.modes.ReverseInputSearchMode
import net.dhleong.judo.net.CommonsNetConnection
import net.dhleong.judo.net.Connection
import net.dhleong.judo.trigger.TriggerManager
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

    private val buffer = InputBuffer()
    private val sendHistory = InputHistory(buffer)
    private val completions = CompletionSourceFacade.create()

    private val normalMode = NormalMode(this, buffer, sendHistory)

    private val modes = sequenceOf(

        InsertMode(this, buffer, completions),
        normalMode,
        PythonCmdMode(this),
        ReverseInputSearchMode(this, buffer, sendHistory)

    ).fold(HashMap<String, Mode>(), { map, mode ->
        map[mode.name] = mode
        map
    })

    private var currentMode: Mode = normalMode

    private var running = true

    private var connection: Connection? = null

    init {
        activateMode(currentMode)
    }

    override fun connect(address: String, port: Int) {
        disconnect()
        echo("Connecting to $address:$port...")

//        val logFile = File("log.txt")

        val connection = CommonsNetConnection(address, port, renderer.terminalType)
        connection.setWindowSize(renderer.windowWidth, renderer.windowHeight)
        connection.onDisconnect = { echo("Disconnected from $connection") }
        connection.onError = { appendError(it, "NETWORK ERROR: ")}
        connection.forEachLine { buffer, count ->
//            logFile.appendText(String(buffer, 0, count))
//            logFile.appendText("{PACKET_BREAK}")
            appendOutput(buffer, count)
            processOutput(stripAnsi(buffer, count))
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

        if (!fromMap) {
            // record it even if we couldn't send it
            sendHistory.push(toSend)

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
        if (stroke.keyCode == KeyEvent.VK_ESCAPE) {
            activateMode(normalMode)
            return
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

    override fun quit() {
        connection?.close()
        renderer.close()
        running = false
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
        currentMode = mode
        mode.onEnter()

        updateInputLine()

        if (mode is BaseCmdMode) {
            renderer.updateStatusLine(":", 1)
        } else {
            renderer.updateStatusLine("[${mode.name.toUpperCase()}]")
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

    private fun appendError(e: Throwable, prefix: String = "") {
        renderer.appendOutput("$prefix${e.message}")
        e.stackTrace.map { "  $it" }
            .forEach { renderer.appendOutput(it) }
        e.cause?.let {
            appendError(it, "Caused by: ")
        }
    }

    internal fun appendOutput(buffer: CharArray, count: Int) {
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
                        buffer.substring(lastLineEnd, i),
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
                    buffer.substring(lastLineEnd, count),
                    isPartialLine = true
                )
            }
        }
    }
}


private fun CharArray.substring(start: Int, end: Int) =
    String(this, start, end - start)
