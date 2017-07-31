package net.dhleong.judo.modes

import net.dhleong.judo.ALL_SETTINGS
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRendererInfo
import net.dhleong.judo.Setting
import net.dhleong.judo.complete.CompletionSuggester
import net.dhleong.judo.complete.DumbCompletionSource
import net.dhleong.judo.event.EventHandler
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.input.KeyMapping
import net.dhleong.judo.input.MutableKeys
import net.dhleong.judo.input.keys
import net.dhleong.judo.logging.ILogManager
import net.dhleong.judo.motions.toEndMotion
import net.dhleong.judo.motions.toStartMotion
import net.dhleong.judo.util.Clearable
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
            "alias(inputSpec: String/Pattern, outputSpec: String)",
            "alias(inputSpec: String/Pattern, handler: Fn)",
            "@alias(inputSpec: String/Pattern)"
        ),
        """
        Create a text alias. You may use re.compile() to match against
        a regular expression, eg:
            import re
            alias(re.compile("^study (.*)"), "say I'd like to study '$1'")
        """.trimIndent()
    ),

    "event" to buildHelp(
        listOf(
            "event(eventName: String, handler: Fn)",
            "@event(eventName: String)"
        ),
        """
        Subscribe to an event with the provided handler.
        Available events:

         "Name": (args) Description
         --------------------------
         "CONNECTED"      ():            Connected to the server
         "DISCONNECTED"   ():            Disconnected to the server
         "GMCP ENABLED    ():            The server declared support for GMCP
         "GMCP            (name, value): A GMCP event was sent by the server
         "GMCP:{pkgName}" (value):       The server sent the value of the GMCP
                                         package {pkgName} (ex: "GMCP:room.info")
         "MSDP ENABLED"   ():            The server declared support for MSDP
         "MSDP"           (name, value): An MSDP variable was sent by the server.
         "MSDP:{varName}" (value):       The server sent the value of the MSDP
                                         variable {varName} (ex: "MSDP:COMMANDS")
        """.trimIndent()
    ),

    "prompt" to buildHelp(
        listOf(
            "prompt(inputSpec: String/Pattern, outputSpec: String)",
            "prompt(inputSpec: String/Pattern, handler: Fn)",
            "@prompt(inputSpec: String/Pattern)"
        ),
        """
        Prepare a prompt to be displayed in the status area.
        See :help alias for more about inputSpec.
        """.trimIndent()
    ),

    "trigger" to buildHelp(
        listOf(
            "trigger(inputSpec: String/Pattern, handler: Fn)",
            "trigger(inputSpec: String/Pattern, options: String, handler: Fn)",
            "@trigger(inputSpec: String/Pattern, options: String)",
            "@trigger(inputSpec: String/Pattern)"
        ),
        """
        Declare a trigger. See :help alias for more about inputSpec.
        `options` is a space-separated string that may contain any of:
         color - Keep color codes in the values passed to the handler
        """.trimIndent()
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

    "hsplit" to buildHelp(
        listOf(
            "hsplit(rows: Int) -> Window",
            "hsplit(perc: Float) -> Window"
        ),
        """
        Create a new window by splitting the active window. The new window
        will be `rows` tall, or `perc` % of the active window's height.
        The resulting Window object has the following attributes and methods:
            id - The window's unique, numeric ID
            buffer - The window's underlying Buffer object
            height - The height of the window in rows
            width - The width of the window in columns
            close() - Close the window
            resize(width, height) - Resize the window

        A Buffer object has the following attributes and methods:
            id - The buffer's unique, numeric ID
            append(line: String) - Append a line to the buffer
            clear() - Remove all lines from the buffer
            set(lines: String[]) - Replace the buffer's contents
                                   with the given lines list
        Buffer also supports len() to get the number of lines
        """.trimIndent()
    ),

    "input" to buildHelp(
        listOf(
            "input() -> String",
            "input(prompt: String) -> String"
        ),
        """Request a string from the user, returning whatever they typed.
          |NOTE: Unlike the equivalent function in Vim, input() DOES NOT currently
          |consume pending input from mappings.
        """.trimMargin()
    ),

    "isConnected" to buildHelp(
        "isConnected() -> Boolean",
        "Check if connected."
    ),

    "load" to buildHelp(
        "load(pathToFile: String)",
        "Load and execute a script."
    ),

    "logToFile" to buildHelp(
        "logToFile(pathToFile: String, options: String)",
        """Enable logging to the given file with the given options. `options` is
          |a space-separated string that may contain any of:
          | append - Append output to the given file if it already exists, instead
          |          of replacing it
          | raw - Output the raw data received from the server, including ANSI codes
          | plain - Output the plain text received from the server, with no coloring
          | html - Output the data received from the server formatted as HTML.
        """.trimMargin()
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

    "config" to buildHelp(
        listOf(
            "config(setting: String, value)",
            "config(setting: String)",
            "config"
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

    "unsplit" to buildHelp(
        "unsplit()",
        "Remove any split windows"
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

    private val mapClearable = MapClearable(judo)
    private val clearQueue = ArrayList<QueuedClear<*>>()
    protected var currentClearableContext: String? = null

    private val suggester = CompletionSuggester(DumbCompletionSource(normalize = false).apply {
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
        suggester.reset()
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

    fun logToFile(path: String, options: String = "append plain") {
        val mode = when {
            options.contains("append", ignoreCase = true) -> ILogManager.Mode.APPEND
            else -> ILogManager.Mode.REPLACE
        }
        val format = when {
            options.contains("html", ignoreCase = true) -> ILogManager.Format.HTML
            options.contains("raw", ignoreCase = true) -> ILogManager.Format.RAW
            else -> ILogManager.Format.PLAIN
        }

        // TODO resolve relative paths relative to the lastLoaded file?
        judo.logging.configure(File(path), format, mode)
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

    fun readFile(file: File) {
        if (!(file.exists() && file.canRead())) {
            throw IllegalArgumentException("Unable to load $file")
        }

        file.inputStream().use {
            readFile(file, it)
        }
    }

    open fun readFile(file: File, inputStream: InputStream) {
        if (file != USER_CONFIG_FILE) {
            lastReadFile = file
        }

        val context = file.absolutePath
        withClearableContext(context) {
            clearQueuedForContext(context)

            readFile(file.name, inputStream)
        }
    }

    open fun reload() {
        lastReadFile?.let {
            // clear any split windows
            judo.tabpage.unsplit()

            readFile(it)
            judo.echo("Reloaded $it")
            return
        }

        judo.echo("No files read; nothing to reload")
    }

    protected fun config(args: Array<Any>) =
        when (args.size) {
            1 -> echoSettingValue(args[0] as String)
            2 -> {
                val settingName = args[0] as String
                config(settingName, args[1])
                echoSettingValue(settingName)
            }

            else -> {
                judo.echo("Settings")
                judo.echo("========")

                ALL_SETTINGS
                    .filter { it.value.description.isNotEmpty() }
                    .map { it.key }
                    .forEach(this::echoSettingValue)
            }
        }

    private fun echoSettingValue(settingName: String) =
        withSetting(settingName) { setting ->
            val value = judo.state[setting]
            val isDefaultFlag =
                if (value == setting.default) " (default)"
                else ""

            val valueDisp =
                if (value is String) """"$value""""
                else value

            judo.echo("${setting.userName} = $valueDisp$isDefaultFlag")
        }

    fun config(settingName: String, value: Any) {
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
                judo.echoRaw()
                judo.echoRaw(judo.aliases)
                true
            }

            "help" -> {
                showHelp()
                true
            }

            "cmap" -> {
                judo.echoRaw()
                judo.printMappings("cmd")
                true
            }

            "imap" -> {
                judo.echoRaw()
                judo.printMappings("insert")
                true
            }

            "nmap" -> {
                judo.echoRaw()
                judo.printMappings("normal")
                true
            }

            "trigger" -> {
                judo.echoRaw()
                judo.echoRaw(judo.triggers)
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
            commands.forEach { judo.echoRaw(it) }
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
                judo.echoRaw(line.toString())
                line.setLength(0)
            }
        }
    }

    internal fun showHelp(command: String) {
        COMMAND_HELP[command]?.let {
            it.split("\n").forEach { judo.echoRaw(it) }
            return
        }

        judo.echoRaw("No such command: $command")
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

    protected fun clearQueuedForContext(context: String) {
        val iter = clearQueue.iterator()
        while (iter.hasNext()) {
            val candidate = iter.next()
            if (candidate.context == context) {
                candidate.clear()
                iter.remove()
            }
        }
    }

    protected fun queueAlias(specOriginal: String) =
        enqueueClear(judo.aliases, specOriginal)

    protected fun queueEvent(event: String, handler: EventHandler) =
        enqueueClear(judo.events, event to handler)

    protected fun queueMap(mode: String, keysFrom: String) =
        enqueueClear(mapClearable, mode to keysFrom)

    protected fun queuePrompt(spec: String) =
        enqueueClear(judo.prompts, spec)

    protected fun queueTrigger(spec: String) =
        enqueueClear(judo.triggers, spec)

    private fun <T> enqueueClear(source: Clearable<T>, entry: T) =
        currentClearableContext?.let {
            clearQueue.add(QueuedClear(it, source, entry))
        }

    protected inline fun withClearableContext(context: String, block: () -> Unit) {
        val oldContext = currentClearableContext
        currentClearableContext = context
        try {
            block()
        } finally {
            currentClearableContext = oldContext
        }
    }

}

class ScriptExecutionException(traceback: String)
    : RuntimeException(traceback)


internal class MapClearable(private val judo: IJudoCore) : Clearable<Pair<String, String>> {
    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun clear(entry: Pair<String, String>) {
        val (mode, keys) = entry
        judo.unmap(mode, keys)
    }
}

internal class QueuedClear<T>(
    val context: String,
    val source: Clearable<T>,
    val entry: T
) {
    fun clear() {
        source.clear(entry)
    }
}
