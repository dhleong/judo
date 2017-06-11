package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.complete.CompletionSuggester
import net.dhleong.judo.complete.DumbCompletionSource
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.util.hasCtrl
import java.awt.event.KeyEvent
import java.io.File
import java.io.InputStream
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

val USER_HOME = System.getProperty("user.home")!!
val USER_CONFIG_FILE = File("$USER_HOME/.config/judo/init.py").absoluteFile!!

private fun buildHelp(usage: String, description: String): String =
    buildHelp(listOf(usage), description)
private fun buildHelp(usages: Iterable<String>, description: String): String =
    with(StringBuilder()) {
        var maxUsageLength = 0
        usages.forEach {
            appendln(it)
            maxUsageLength = maxOf(maxUsageLength, it.length)
        }
        appendln("=".repeat(maxUsageLength))
        appendln(description)

        toString()
    }

private val COMMAND_HELP = mutableMapOf(
    "alias" to buildHelp(
        listOf(
            "alias(inputSpec: String, outputSpec: String)",
            "alias(inputSpec: String, handler: Fn)",
            "@alias(inputSpec: String)"
        ),
        "Create a text alias."
    ),

    "prompt" to buildHelp(
        listOf(
            "prompt(inputSpec: String, outputSpec: String)",
            "prompt(inputSpec: String, handler: Fn)",
            "@prompt(inputSpec: String)"
        ),
        "Prepare a prompt to be displayed in the status area."
    ),

    "trigger" to buildHelp(
        listOf(
            "trigger(inputSpec: String, handler: Fn)",
            "@trigger(inputSpec: String)"
        ),
        "Declare a trigger."
    ),

    "connect" to buildHelp(
        "connect(host: String, port: Int)",
        "Connect to a server."
    ),

    "createUserMode" to buildHelp(
        "createUserMode(modeName: String)",
        "Create a new mode with the given name. Mappings can be added to it" +
        "using the createMap() function"
    ),

    "disconnect" to buildHelp(
        "disconnect()",
        "Disconnect from the server."
    ),

    "enterMode" to buildHelp(
        "enterMode(modeName: String)",
        "Enter the mode with the given name."
    ),

    "exitMode" to buildHelp(
        "exitMode()",
        "Enter the current mode."
    ),

    "echo" to buildHelp(
        "echo(...)",
        "Print some output to the screen locally."
    ),

    "load" to buildHelp(
        "load(pathToFile: String)",
        "Load and execute a script."
    ),

    "input" to buildHelp(
        listOf(
            "input() -> String",
            "input(prompt: String) -> String"
        ),
        "Request a string from the user, returning whatever they typed.\n" +
        "NOTE: Unlike the equivalent function in Vim, input() DOES NOT\n" +
        "currently consume pending input from mappings."
    ),

    "isConnected" to buildHelp(
        "isConnected() -> Boolean",
        "Check if connected."
    ),

    "quit" to buildHelp(
        "quit()",
        "Exit Judo."
    ),

    "reconnect" to buildHelp(
        "reconnect()",
        "Repeat the last connect()"
    ),

    "reload" to buildHelp(
        "reload()",
        "Reload the last-loaded script file."
    ),

    "send" to buildHelp(
        "send(text: String)",
        "Send some text to the connected server."
    ),

    "startInsert" to buildHelp(
        "startInsert()",
        "Enter insert mode as if by pressing `i`"
    ),

    "stopInsert" to buildHelp(
        "stopInsert()",
        "Exit insert mode as soon as possible"
    ),

    "unalias" to buildHelp(
        "unalias(inputSpec: String)",
        "Delete the alias with the specified inputSpec"
    ),

    "untrigger" to buildHelp(
        "untrigger(inputSpec: String)",
        "Delete the trigger with the specified inputSpec"
    )

).apply {
    val kinds = sequenceOf("", "c", "i", "n")
    val mapTypes = kinds.flatMap { sequenceOf("${it}map", "${it}noremap") }
    val unmapTypes = kinds.map { "${it}unmap" }

    val mapHelp = buildHelp(
        mapTypes.map {
            "$it(inputKeys: String, outputKeys: String)"
        }.toList()
            + "createMap(modeName: String, inputKeys: String, outputKeys: String)",
        "Create a mapping in a specific mode from inputKeys to outputKeys"
    )

    val unmapHelp = buildHelp(
        unmapTypes.map {
            "$it(inputKeys: String)"
        }.toList()
            + "deleteMap(modeName: String, inputKeys: String)",
        "Delete a mapping in the specific mode with inputKeys"
    )

    mapTypes.forEach {
        put(it, mapHelp)
    }
    put("createMap", mapHelp)

    unmapTypes.forEach {
        put(it, unmapHelp)
    }
    put("deleteMap", unmapHelp)
}

abstract class BaseCmdMode(
    judo: IJudoCore,
    buffer: InputBuffer,
    val history: IInputHistory
) : BaseModeWithBuffer(judo, buffer),
    MappableMode {

    override val userMappings = KeyMapping()
    override val name = "cmd"

    private val suggester = CompletionSuggester(DumbCompletionSource().apply {
        COMMAND_HELP.keys.forEach(this::process)
        process("help")
    })

    val mapping = KeyMapping(
        keys("<up>") to { _ -> history.scroll(-1) },
        keys("<down>") to { _ -> history.scroll(1) },

        keys("<ctrl a>") to motionAction(toStartMotion()),
        keys("<ctrl e>") to motionAction(toEndMotion())
    )
    private val input = MutableKeys()

    private var lastReadFile: File? = null

    override fun onEnter() {
        clearBuffer()
    }

    override fun feedKey(key: KeyStroke, remap: Boolean, fromMap: Boolean) {
        when {
            key.keyCode == KeyEvent.VK_ENTER -> {
                val code = buffer.toString().trim()
                when (code) {
                    "q", "q!", "qa", "qa!" -> {
                        judo.quit()
                        return
                    }
                }

                if (code == "help") {
                    showHelp()
                } else if (code.startsWith("help")) {
                    showHelp(code.substring(5))
                } else if (!(code.contains('(') && code.contains(')')) && code !in COMMAND_HELP) {
                    showHelp(code)
                } else if (code in COMMAND_HELP) {
                    // no args needed, so just implicitly handle for convenience
                    execute("$code()")
                    history.push(code)
                } else {
                    execute(code)
                    history.push(code)
                }

                clearBuffer()
                exitMode()
                return
            }

            key.keyChar == 'c' && key.hasCtrl() -> {
                clearBuffer()
                exitMode()
                return
            }

            // NOTE: ctrl+i == tab
            key.keyCode == KeyEvent.VK_TAB
                    || key.keyChar == 'i' && key.hasCtrl() -> {
                performTabCompletionFrom(key, suggester)
                return
            }
        }

        // input changed; suggestions go away
        suggester.reset()

        // handle key mappings
        if (tryMappings(key, remap, input, mapping, userMappings)) {
            return
        }

        if (key.hasCtrl()) {
            // ignore
            return
        }

        insertChar(key)
    }

    fun load(pathToFile: String) {
        val file = File(pathToFile)
        readFile(file)
        judo.echo("Loaded $file")
    }

    fun readFile(file: File) {
        if (!(file.exists() && file.canRead())) {
            throw IllegalArgumentException("Unable to load $file")
        }

        lastReadFile = file
        file.inputStream().use {
            readFile(file.name, it)
        }
    }

    fun reload() {
        lastReadFile?.let {
            readFile(it)
            judo.echo("Reloaded $it")
            return
        }

        judo.echo("No files read; nothing to reload")
    }

    private fun showHelp() {
        // TODO columns?
        COMMAND_HELP.keys.forEach { judo.echo(it) }
    }

    private fun showHelp(command: String) {
        COMMAND_HELP[command]?.let {
            it.split("\n").forEach { judo.echo(it) }
            return
        }

        judo.echo("No such command: $command")
    }

    private fun exitMode() {
        judo.exitMode()
    }

    /**
     * Insert a key stroke at the current cursor position
     */
    private fun insertChar(key: KeyStroke) {
        val wasEmpty = buffer.isEmpty()
        buffer.type(key)
        if (buffer.isEmpty() && wasEmpty) {
            exitMode()
        }
    }

    abstract fun execute(code: String)

    abstract protected fun readFile(fileName: String, stream: InputStream)

    private fun clearBuffer() {
        buffer.clear()
        input.clear()
    }
}
