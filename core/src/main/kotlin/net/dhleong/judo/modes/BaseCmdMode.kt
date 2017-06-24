package net.dhleong.judo.modes

import net.dhleong.judo.ALL_SETTINGS
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRendererInfo
import net.dhleong.judo.Setting
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
import net.dhleong.judo.util.hash
import java.awt.event.KeyEvent
import java.io.File
import java.io.InputStream
import javax.swing.KeyStroke

/**
 * @author dhleong
 */

val USER_HOME = System.getProperty("user.home")!!
val USER_CONFIG_DIR = File("$USER_HOME/.config/judo/").absoluteFile!!
val USER_CONFIG_FILE = File("${USER_CONFIG_DIR.absolutePath}/init.py").absoluteFile!!

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

    "complete" to buildHelp(
        "complete(text: String)",
        """Feed some text into the text completion system.
          |NOTE: This does not yet guarantee that the provided words will
          |be suggested in the sequence provided, but it may in the future.
        """.trimMargin()
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

    "normal" to buildHelp(
        listOf(
            "normal(keys: String)",
            "normal(keys: String, remap: Boolean)"
        ),
        """Process [keys] as though they were typed by the user in normal mode.
          |To perform this operation with remaps disabled (as in nnoremap), pass
          |False for the second parameter.
        """.trimMargin()
    ),

    "persistInput" to buildHelp(
        listOf(
            "persistInput()",
            "persistInput(path: String)"
        ),
        """Enable input history persistence for the current world, optionally
          |providing the path to save the history. If not provided, it will pick
          |a path in the ~/.config/judo directory with a filename based on the
          |currently-connected world (which means this should be called AFTER a
          |call to connect()).
          |This will immediately attempt to import input history from the given
          |file, and writes the new history on disconnect. Persistence is also
          |disabled on disconnect, so you'll need to call this again the next time
          |you connect.
        """.trimMargin()
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
        "Reload the last-loaded, non-MYJUDORC script file."
    ),

    "send" to buildHelp(
        "send(text: String)",
        "Send some text to the connected server."
    ),

    "set" to buildHelp(
        listOf(
            "set(setting: String, value)",
            "set(setting: String)",
            "set"
        ),
        "Set or get the value of a setting, or list all settings"
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
    private val rendererInfo: JudoRendererInfo,
    val history: IInputHistory
) : BaseModeWithBuffer(judo, buffer),
    MappableMode,
    StatusBufferProvider {

    override val userMappings = KeyMapping()
    override val name = "cmd"

    private val suggester = CompletionSuggester(DumbCompletionSource().apply {
        COMMAND_HELP.keys.forEach(this::process)
        process("help")
    })

    val mapping = KeyMapping(
        keys("<up>") to { _ -> history.scroll(-1, clampCursor = false) },
        keys("<down>") to { _ -> history.scroll(1, clampCursor = false) },

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

                clearBuffer()
                exitMode()

                // some special no-arg "commands"
                if (handleNoArgListingCommand(code)) {
                    return
                }

                if (code.startsWith("help")) {
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

    override fun renderStatusBuffer(): String = ":$buffer"
    override fun getCursor(): Int = buffer.cursor + 1

    fun load(pathToFile: String) {
        val file = File(pathToFile)
        readFile(file)
        judo.echo("Loaded $file")
    }

    fun persistInput() {
        judo.connection?.let {
            val fileName = hash(it.toString())
            val filePath = "$USER_CONFIG_DIR/input-history/$fileName"
            judo.persistInput(File(filePath))
            return
        }

        throw IllegalStateException("You must be connected to use persistInput() without args")
    }

    open fun readFile(file: File) {
        if (!(file.exists() && file.canRead())) {
            throw IllegalArgumentException("Unable to load $file")
        }

        if (file != USER_CONFIG_FILE) {
            lastReadFile = file
        }

        file.inputStream().use {
            readFile(file.name, it)
        }
    }

    open fun reload() {
        lastReadFile?.let {
            readFile(it)
            judo.echo("Reloaded $it")
            return
        }

        judo.echo("No files read; nothing to reload")
    }

    protected fun set(args: Array<Any>) =
        when (args.size) {
            1 -> echoSettingValue(args[0] as String)
            2 -> {
                val settingName = args[0] as String
                set(settingName, args[1])
                echoSettingValue(settingName)
            }

            else -> {
                judo.echo("Settings")
                judo.echo("========")
                ALL_SETTINGS.keys.forEach(this::echoSettingValue)
            }
        }

    private fun echoSettingValue(settingName: String) =
        withSetting(settingName) { setting ->
            val value = setting.read(judo.state)
            val isDefaultFlag =
                if (value == setting.default) " (default)"
                else ""

            val valueDisp =
                if (value is String) """"$value""""
                else value

            judo.echo("${setting.userName} = $valueDisp$isDefaultFlag")
        }

    fun set(settingName: String, value: Any) {
        withSetting(settingName) {
            if (!it.type.isAssignableFrom(value.javaClass)) {
                throw ScriptExecutionException(
                    "$value is invalid for setting `$settingName` (requires: ${it.type})")
            }

            judo.state[it] = it.type.cast(value) as Any
        }
    }

    private inline fun withSetting(settingName: String, block: (Setting<Any>) -> Unit) {
        ALL_SETTINGS[settingName]?.let {
            @Suppress("UNCHECKED_CAST")
            block(it as Setting<Any>)
            return
        }

        throw ScriptExecutionException("No such setting `$settingName`")
    }

    internal fun handleNoArgListingCommand(command: String): Boolean =
        when (command) {
            "alias" -> {
                judo.echo()
                judo.echo(judo.aliases)
                true
            }

            "help" -> {
                showHelp()
                true
            }

            "cmap" -> {
                judo.echo()
                judo.printMappings("cmd")
                true
            }

            "imap" -> {
                judo.echo()
                judo.printMappings("insert")
                true
            }

            "nmap" -> {
                judo.echo()
                judo.printMappings("normal")
                true
            }

            "trigger" -> {
                judo.echo()
                judo.echo(judo.triggers)
                true
            }

            else -> false
        }

    internal fun showHelp() {
        val commands = COMMAND_HELP.keys.sorted().toList()
        val longest = commands.asSequence().map { it.length }.max()!!
        val colWidth = longest + 2

        if (colWidth >= rendererInfo.windowWidth) {
            // super small renderer (whaaat?)
            // just be lazy
            commands.forEach { judo.echo(it) }
            return
        }

        val cols = rendererInfo.windowWidth / colWidth
        val line = StringBuilder(cols * colWidth)
        var word = 0
        for (name in commands) {
            line.append(name)
            for (i in 0..(colWidth - name.length - 1)) {
                line.append(' ')
            }

            ++word

            if (word >= cols) {
                // end of the line; dump it and start over
                word = 0
                judo.echo(line.toString())
                line.setLength(0)
            }
        }
    }

    internal fun showHelp(command: String) {
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

class ScriptExecutionException(traceback: String)
    : RuntimeException(traceback)
