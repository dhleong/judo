package net.dhleong.judo

import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.Keys
import net.dhleong.judo.modes.BaseCmdMode
import net.dhleong.judo.modes.InsertMode
import net.dhleong.judo.modes.MappableMode
import net.dhleong.judo.modes.NormalMode
import net.dhleong.judo.modes.PythonCmdMode
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

class JudoCore(val renderer: JudoRenderer) : IJudoCore {

    override val aliases = AliasManager()

    private val buffer = InputBuffer()

    private val normalMode = NormalMode(this, buffer)
    private val modes = mapOf(
        "insert" to InsertMode(this, buffer),
        "normal" to normalMode,
        "cmd" to PythonCmdMode(this)
    )

    private var currentMode: Mode = normalMode

    private var running = true

    private var connection: Any? = null // FIXME TODO

    init {
        activateMode(currentMode)
    }

    override fun echo(vararg objects: Any?) {
        // TODO colors?
        renderer.appendOutputLine(objects.joinToString(" "))
    }

    override fun enterMode(modeName: String) {
        val mode = modes[modeName]
        if (mode != null) {
            activateMode(mode)
        } else {
            throw IllegalArgumentException("No such mode `$modeName`")
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

        throw IllegalArgumentException("No such mode $mode")
    }

    override fun send(text: String) {
        val toSend = aliases.process(text)
        connection?.let {
            TODO("implement send")
//            return
        }

        appendError(Error("Not connected."))
    }

    override fun feedKey(stroke: KeyStroke, remap: Boolean) {
        echo("## feed $stroke")
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
            renderer.updateInputLine(buffer.toString(), buffer.cursor)
        }
    }

    override fun quit() {
        renderer.close()
        running = false
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

    private fun activateMode(mode: Mode) {
        echo("Activate ${mode.name}")
        currentMode = mode
        mode.onEnter()

        renderer.updateInputLine(buffer.toString(), buffer.cursor)

        if (mode is BaseCmdMode) {
            echo("Activate CMD")
            renderer.updateStatusLine(":", 1)
        } else {
            renderer.updateStatusLine("[${mode.name.toUpperCase()}]")
        }
    }

    private fun appendError(e: Throwable, prefix: String = "") {
        renderer.appendOutputLine("$prefix${e.message}")
        e.stackTrace.map { it.toString() }
            .forEach(renderer::appendOutputLine)
    }

}

