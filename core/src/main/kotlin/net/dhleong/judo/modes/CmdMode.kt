package net.dhleong.judo.modes

import kotlinx.coroutines.runBlocking
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRenderer
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.event.EventHandler
import net.dhleong.judo.event.handler
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.net.createURI
import net.dhleong.judo.prompt.AUTO_UNIQUE_GROUP_ID
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.script.JudoScriptDoc
import net.dhleong.judo.script.JudoScriptingEntity
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.script.doc
import net.dhleong.judo.util.PatternProcessingFlags
import net.dhleong.judo.util.PatternSpec
import java.io.File
import java.io.InputStream

/**
 * @author dhleong
 */
class CmdMode(
    judo: IJudoCore,
    private val ids: IdManager,
    inputBuffer: InputBuffer,
    private val renderer: JudoRenderer,
    history: IInputHistory,
    private var completions: CompletionSource,
    userConfigDir: File,
    userConfigFile: File,
    private val engineFactory: ScriptingEngine.Factory,
    private val inputCmdBuffer: InputBuffer,
    private val inputCmdHistory: IInputHistory
) : BaseCmdMode(
    judo,
    inputBuffer, renderer,
    history,
    userConfigDir, userConfigFile
) {
    private val engine by lazy {
        engineFactory.create().apply {
            onPreRegister()
            init()
            onPostRegister()

            completionSource.process("help")
        }
    }

    private val myRegisteredFns = mutableSetOf<String>()
    private val myRegisteredVars = mutableMapOf<String, JudoScriptingEntity>()

    override val registeredFns: MutableSet<String>
        get() {
            engine // ensure initialized
            return myRegisteredFns
        }
    override val registeredVars: MutableMap<String, JudoScriptingEntity>
        get() {
            engine // ensure initialized
            return myRegisteredVars
        }

    fun interrupt() {
        engine.interrupt()
    }

    override fun execute(code: String) {
        engine.execute(code)
    }

    override fun executeImplicit(fnName: String) {
        engine.execute(engineFactory.formatFnCall(fnName))
    }

    override fun readFile(file: File, inputStream: InputStream) {
        engine.onPreReadFile(file, inputStream)
        super.readFile(file, inputStream)
    }

    override fun readFile(fileName: String, stream: InputStream) {
        engine.readFile(fileName, stream)
    }

    override fun callableToAliasProcessor(fromScript: Any): AliasProcesser =
        engine.callableToAliasProcessor(fromScript)

    override val supportsDecorators: Boolean
        get() = engineFactory.supportsDecorators

    override fun reload() {
        engine.onPreReload()
        super.reload()
        engine.onPostReload()
    }

    private fun createMap(modeName: String, fromKeys: String, mapTo: Any, remap: Boolean) {
        when (mapTo) {
            is String -> judo.map(
                modeName,
                fromKeys,
                mapTo,
                remap
            )

            else -> {
                val fn = try {
                    engine.callableToFunction0<Unit>(mapTo)
                } catch (e: Throwable) {
                    throw IllegalArgumentException(
                        "Unexpected map-to value $mapTo " +
                            "(${mapTo.javaClass} / ${mapTo.javaClass.genericInterfaces.toList()})",
                        e
                    )
                }

                judo.map(
                    modeName,
                    fromKeys,
                    fn,
                    mapTo.toString()
                )
            }
        }

        queueMap(modeName, fromKeys)
    }

    private fun defineEvent(eventName: String, handlerFromScript: Any) {
        val argCount = engine.callableArgsCount(handlerFromScript)
        val handler: EventHandler = when (argCount) {
            0 -> {
                val fn0 = engine.callableToFunction0<Any>(handlerFromScript)
                handler { fn0() }
            }

            1 -> {
                val fn1 = engine.callableToFunction1(handlerFromScript)
                object : EventHandler {
                    override fun invoke(arg: Any?) { fn1(arg) }
                }
            }

            else -> {
                val fnN = engine.callableToFunctionN(handlerFromScript)

                object : EventHandler {
                    override fun invoke(rawArg: Any?) {
                        if (rawArg !is Array<*>) {
                            throw ScriptExecutionException(
                                "$handlerFromScript expected $argCount arguments, but event arg was not an array")
                        }

                        @Suppress("UNCHECKED_CAST")
                        fnN(rawArg as Array<Any>)
                    }
                }
            }
        }

        judo.events.register(eventName, handler)
        queueEvent(eventName, handler)
    }

    /** User-input group number */
    private fun tryDefinePrompt(group: Int, prompt: PatternSpec, handler: Any) {
        if (group <= 0) {
            throw IllegalArgumentException("group must be > 0")
        }
        definePrompt(group, prompt, handler)
    }
    private fun definePrompt(group: Int, prompt: PatternSpec, handler: Any) {
        queuePrompt(prompt.original)
        if (handler is String) {
            judo.prompts.define(prompt, handler, group)
        } else {
            val processor = callableToAliasProcessor(handler)
            judo.prompts.define(prompt, processor, group)
        }
    }

    private fun defineTrigger(trigger: PatternSpec, handler: Any) {
        queueTrigger(trigger.original)
        val fn = engine.callableToFunctionN(handler)
        judo.triggers.define(trigger) { args ->
            @Suppress("UNCHECKED_CAST")
            fn(args as Array<Any>)
        }
    }

    private suspend fun feedKeys(userInput: Array<Any>, mode: String) {
        val keys = userInput[0] as? String ?: throw IllegalArgumentException("[keys] must be a String")

        val remap =
            if (userInput.size == 1) true
            else userInput[1] as? Boolean ?: throw IllegalArgumentException("[remap] must be a Boolean")

        judo.feedKeys(keys, remap, mode)
    }

    private suspend fun readInput(prompt: String): String? {
        // TODO user-provided completions?
        val inputMode = ScriptInputMode(
            judo, completions,
            inputCmdBuffer, inputCmdHistory,
            prompt
        )
        judo.enterMode(inputMode)
        return inputMode.awaitResult()
    }

    private fun <T> ScriptingEngine.registerConst(name: String, doc: JudoScriptDoc, value: T) {
        registerVar(JudoScriptingEntity.Constant(name, doc, value))
    }

    private fun <R> ScriptingEngine.registerFn(name: String, doc: JudoScriptDoc, fn: kotlin.Function<R>) {
        registerVar(JudoScriptingEntity.Function(name, doc, fn))
        myRegisteredFns.add(name)
    }

    private fun ScriptingEngine.registerVar(entity: JudoScriptingEntity) {
        register(entity)
        myRegisteredVars[entity.name] = entity
        completionSource.process(entity.name)
    }

    private fun ScriptingEngine.init() {
        initConsts()
        initCore()

        initAliases()
        initConnection()
        initEvents()
        initFiles()
        initKeymaps()
        initModes()
        initPrompts()
        initTriggers()
        initWindows()
    }

    private fun ScriptingEngine.initConsts() {
        registerConst(
            "MYJUDORC",
            doc {
                body {
                    "Path to the Judo config file"
                }
            },
            userConfigFile.absolutePath
        )

        registerConst(
            "judo",
            doc {
                body { "Reference to the core Judo scripting object" }
            },
            wrapCore(judo)
        )
    }

    private fun ScriptingEngine.initCore() {
        registerFn<Unit>(
            "config",
            doc {
                usage {
                    arg("setting", "String")
                    arg("value", "Any")
                }
                usage { arg("setting", "String") }
                usage { /* no-arg */ }

                body { "Set or get the value of a setting, or list all settings" }
            }
        ) { args: Array<Any> -> config(args) }

        registerFn(
            "complete",
            doc {
                usage { arg("text", "String") }
                body { """
                    Feed some text into the text completion system.
                    NOTE: This does not yet guarantee that the provided words will
                    be suggested in the sequence provided, but it may in the future.
                """.trimIndent() }
            },
            judo::seedCompletion
        )

        registerFn<Unit>(
            "echo",
            doc {
                usage { withVarArgs() }
                body { "Echo some transient text to the screen locally." }
            }
        ) { args: Array<Any> -> judo.echo(*args) }

        registerFn<Unit>(
            "print",
            doc {
                usage { withVarArgs() }
                body { "Print some output into the current buffer locally." }
            }
        ) { args: Array<Any> -> judo.print(*args) }

        registerFn<String?>(
            "input",
            doc {
                usage { returns("String") }
                usage {
                    arg("prompt", "String")
                    returns("String")
                }
                body {
        """Request a string from the user, returning whatever they typed.
          |NOTE: Unlike the equivalent function in Vim, input() DOES NOT currently
          |consume pending input from mappings.
        """.trimMargin()
                }
            }
        ) { args: Array<Any> -> adaptSuspend {
            if (args.isNotEmpty()) {
                readInput(args[0] as String)
            } else {
                readInput("")
            }
        } }

        registerFn<Unit>(
            "normal",
            doc {
                usage { arg("keys", "String") }
                usage {
                    arg("keys", "String")
                    arg("remap", "Boolean")
                }
                body { """
                    Process [keys] as though they were typed by the user in normal mode.
                    To perform this operation with remaps disabled (as in nnoremap), pass
                    False for the second parameter.
                """.trimMargin() }
            }
        ) { args: Array<Any> -> adaptSuspend { feedKeys(args, mode = "normal") } }


        registerFn(
            "redraw",
            doc {
                usage { }
                body { """
                    Force a redraw of the screen; clears any echo()'d output
                """.trimMargin() }
            },
            judo::redraw
        )

        registerFn(
            "quit",
            doc {
                usage { }
                body { "Exit Judo." }
            },
            judo::quit
        )

        registerFn<Unit>(
            "reload",
            doc {
                usage { }
                body { "Reload the last-loaded, non-MYJUDORC script file." }
            }
        ) { reload() }

        registerFn<Unit>(
            "send",
            doc {
                usage { arg("text", "String") }
                body { "Send some text to the connected server." }
            }
        ) { text: String -> judo.send(text, true) }
    }

    private fun ScriptingEngine.initAliases() {
        // aliasing
        registerFn<Unit>(
            "alias",
            doc {
                usage {
                    arg("inputSpec", "String/Pattern")
                    arg("outputSpec", "String")
                }
                usage(decorator = true) {
                    arg("inputSpec", "String/Pattern")
                    arg("handler", "Fn")
                }

                body { """
                    Create a text alias. You may use Regex objects to match against
                    a regular expression. In Python, for example:
                        import re
                        alias(re.compile("^study (.*)"), "say I'd like to study '$1'")
                """.trimIndent() }
            }
        ) { spec: Any, alias: Any ->
            defineAlias(compilePatternSpec(spec, ""), alias)
        }

        registerFn<Unit>(
            "unalias",
            doc {
                usage { arg("inputSpec", "String") }
                body { "Delete the alias with the specified inputSpec" }
            }
        ) { inputSpec: String ->
            judo.aliases.undefine(inputSpec)
        }
    }

    private fun ScriptingEngine.initConnection() {
        registerFn<Unit>(
            "connect",
            doc {
                usage {
                    arg("uri", "String")
                }

                usage {
                    arg("host", "String")
                    arg("port", "Int")
                }

                body { "Connect to a server." }
            }
        ) { args: Array<Any> -> when (args.size) {
            1 -> judo.connect(createURI(args[0] as String))
            2 -> judo.connect(createURI("${args[0]}:${args[1]}"))
        } }

        registerFn(
            "disconnect",
            doc {
                usage { }
                body { "Disconnect from the server." }
            },
            judo::disconnect
        )

        registerFn(
            "isConnected",
            doc {
                usage { returns("Boolean") }
                body { "Check if connected." }
            },
            judo::isConnected
        )

        registerFn(
            "reconnect",
            doc {
                usage { }
                body { "Repeat the last connect()" }
            },
            judo::reconnect
        )
    }

    private fun ScriptingEngine.initEvents() {
        registerFn<Unit>(
            "event",
            doc {
                usage(decorator = true) {
                    arg("eventName", "String")
                    arg("handler", "Fn")
                }

                body { """
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
                """.trimIndent() }
            }
        ) { eventName: String, handler: Any -> defineEvent(eventName, handler) }
    }

    private fun ScriptingEngine.initFiles() {
        registerFn<Unit>(
            "load",
            doc {
                usage { arg("pathToFile", "String") }
                body { "Load and execute a script." }
            }
        ) { pathToFile: String -> load(pathToFile) }

        registerFn<Unit>(
            "logToFile",
            doc {
                usage {
                    arg("pathToFile", "String")
                    arg("options", "String", isOptional = true)
                }
                body {
        """Enable logging to the given file with the given options. `options` is
          |a space-separated string that may contain any of:
          | append - Append output to the given file if it already exists, instead
          |          of replacing it
          | raw - Output the raw data received from the server, including ANSI codes
          | plain - Output the plain text received from the server, with no coloring
          | html - Output the data received from the server formatted as HTML.
        """.trimMargin()
                }
            }
        ) { args: Array<Any> ->
            if (args.size == 1) logToFile(args[0] as String)
            else logToFile(args[0] as String, args[1] as String)
        }

        registerFn<Unit>(
            "persistInput",
            doc {
                usage { }
                usage { arg("path", "String") }
                body {
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
                }
            }
        ) { args: Array<Any> ->
            if (args.isNotEmpty()) {
                judo.persistInput(File(args[0] as String))
            } else {
                persistInput()
            }
        }
    }

    private fun ScriptingEngine.initKeymaps() {
        // mapping functions
        sequenceOf(
            "" to "",
            "c" to "cmd",
            "i" to "insert",
            "n" to "normal"
        ).forEach { (letter, modeName) ->

            registerFn<Unit>(
                "${letter}map",
                doc {
                    usage {
                        arg("inputKeys", "String")
                        arg("output", "String/Fn")
                    }
                    usage { /* no args to list */ }

                    body { "Create a mapping in a specific mode from inputKeys to outputKeys" }
                }
            ) { args: Array<Any> ->
                when (args.size) {
                    0 -> judo.printMappings(modeName)
                    1 -> {
                        // special case; map(mode)
                        judo.printMappings(args[0] as String)
                    }
                    else -> {
                        createMap(modeName, args[0] as String, args[1], true)
                    }
                }
            }

            registerFn<Unit>(
                "${letter}noremap",
                doc {
                    usage {
                        arg("inputKeys", "String")
                        arg("output", "String/Fn")
                    }
                    usage { /* no args to list */ }

                    body { "Create a mapping in a specific mode from inputKeys to outputKeys" }
                }
            ) { inputKeys: String, output: Any ->
                createMap(modeName, inputKeys, output, false)
            }

            registerFn<Unit>(
                "${letter}unmap",
                doc {
                    usage {
                        arg("inputKeys", "String")
                    }

                    body { "Delete a mapping in the specific mode with inputKeys" }
                }
            ) { inputKeys: String ->
                judo.unmap(modeName, inputKeys)
            }
        }

        registerFn<Unit>(
            "createMap",
            doc {
                usage {
                    arg("modeName", "String")
                    arg("inputKeys", "String")
                    arg("outputKeys", "String")
                    arg("remap", "Boolean", isOptional = true)
                }
                body { """
                    Create a mapping in a specific mode from inputKeys to outputKeys.
                    If remap is provided and True, the outputKeys can trigger other mappings.
                    Otherwise, they will be sent as-is.
                """.trimIndent() }
            }
        ) { args: Array<Any> ->
            val remap =
                if (args.size == 4) args[3] as Boolean
                else false
            createMap(args[0] as String, args[1] as String, args[2], remap)
        }

        registerFn<Unit>(
            "deleteMap",
            doc {
                usage {
                    arg("modeName", "String")
                    arg("inputKeys", "String")
                }
                body { "Delete a mapping in the specific mode with inputKeys" }
            }
        ) { modeName: String, inputKeys: String ->
            judo.unmap(modeName, inputKeys)
        }
    }

    private fun ScriptingEngine.initModes() {
        registerFn(
            "createUserMode",
            doc {
                usage { arg("modeName", "String") }
                body { """
                    Create a new mode with the given name. Mappings can be added to it
                    using the createMap function
                """.trimIndent() }
            },
            judo::createUserMode
        )

        registerFn<Unit>(
            "enterMode",
            doc {
                usage { arg("modeName", "String") }
                body { "Enter the mode with the given name." }
            }
        ) { modeName: String -> judo.enterMode(modeName) }

        registerFn<Unit>(
            "exitMode",
            doc {
                usage { }
                body { "Exit the current mode." }
            }
        ) { modeName: String -> judo.enterMode(modeName) }

        registerFn<Unit>(
            "startInsert",
            doc {
                usage {  }
                body { "Enter insert mode as if by pressing `i`" }
            }
        ) { judo.enterMode("insert") }

        registerFn<Unit>(
            "stopInsert",
            doc {
                usage {  }
                body { "Exit insert mode as soon as possible." }
            }
        ) { judo.exitMode() }
    }

    private fun ScriptingEngine.initPrompts() {
        registerFn<Unit>(
            "prompt",
            doc {
                usage {
                    arg("group", "Int", isOptional = true)
                    arg("inputSpec", "Pattern/String")
                    arg("options", "String", flags = PatternProcessingFlags::class.java)
                    arg("outputSpec", "String")
                }

                usage(decorator = true) {
                    arg("group", Integer::class.java, isOptional = true)
                    arg("inputSpec", "Pattern/String")
                    arg("options", "String", flags = PatternProcessingFlags::class.java)
                    arg("handler", "Fn")
                }
                body { """
                    Prepare a prompt to be displayed in the status area.
                    See :help alias for more about `inputSpec`, and :help trigger for more
                    about the optional `options`.

                    You may optionally assign your prompt to a `group` in order to
                    display multiple prompts at once. The number must be greater than
                    zero (0) but is otherwise arbitrary, as long as you are consistent.
                    If `group` is not specified, the prompt will be added to its own group.

                    As long as the matched prompts belong to the same group they will
                    all be displayed, in the order you declared them, in the prompt
                    area. If a prompt from a different group is matched, only prompts
                    from *that* group will be displayed at that point, and so on.
                """.trimIndent() }
            }
        ) { args: Array<Any> -> when (args.size) {
            2 -> definePrompt(
                AUTO_UNIQUE_GROUP_ID,
                compilePatternSpec(args[0], ""), args[1]
            )
            3 -> {
                if (args[0] is Int) {
                    // provided a group
                    tryDefinePrompt(args[0] as Int, compilePatternSpec(args[1], ""), args[2])
                } else {
                    // provided flags
                    definePrompt(
                        AUTO_UNIQUE_GROUP_ID,
                        compilePatternSpec(args[0], args[1] as String), args[2]
                    )
                }
            }
            4 -> tryDefinePrompt(args[0] as Int, compilePatternSpec(args[1], args[2] as String), args[3])
        } }
    }

    private fun ScriptingEngine.initTriggers() {
        registerFn<Unit>(
            "trigger",
            doc {
                usage(decorator = true) {
                    arg("inputSpec", "String/Pattern")
                    arg("options", "String", flags = PatternProcessingFlags::class.java)
                    arg("handler", "Fn")
                }

                body { """
                    Declare a trigger. See :help alias for more about inputSpec.
                    `options` is an optional, space-separated string that may contain any of:
                         color - Keep color codes in the values passed to the handler
                """.trimIndent() }
            }
        ) { args: Array<Any> ->
            if (args.size == 2) {
                defineTrigger(compilePatternSpec(args[0], ""), args[1])
            } else {
                defineTrigger(compilePatternSpec(args[0], args[1] as String), args[2])
            }
        }

        registerFn<Unit>(
            "untrigger",
            doc {
                usage { arg("inputSpec", "String") }
                body { "Delete the trigger with the specified inputSpec" }
            }
        ) { inputSpec: String ->
            judo.triggers.undefine(inputSpec)
        }
    }

    private fun ScriptingEngine.initWindows() {
        val splitHelp = """
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

        registerFn<Any>(
            "hsplit",
            doc {
                usage {
                    arg("rows", "Int")
                    returns("Window")
                }
                usage {
                    arg("perc", "Float")
                    returns("Window")
                }

                body { """
                    Create a new window by splitting the active window. The new window
                    will be `rows` tall, or `perc` % of the active window's height.
                """.trimIndent() + "\n" + splitHelp }
            }
        ) { arg: Any -> dispatchSplit { newBuffer ->
            when (arg) {
                is Int -> judo.tabpage.hsplit(arg, newBuffer)
                is Float -> judo.tabpage.hsplit(arg, newBuffer)
                is Double -> judo.tabpage.hsplit(arg.toFloat(), newBuffer)
                else -> throw IllegalArgumentException()
            }
        } }

        registerFn<Any>(
            "vsplit",
            doc {
                usage {
                    arg("cols", "Int")
                    returns("Window")
                }
                usage {
                    arg("perc", "Float")
                    returns("Window")
                }

                body { """
                    Create a new window by splitting the active window. The new window
                    will be `cols` wide, or `perc` % of the active window's width.
                """.trimIndent() + "\n" + splitHelp }
            }
        ) { arg: Any -> dispatchSplit { newBuffer ->
            when (arg) {
                is Int -> judo.tabpage.vsplit(arg, newBuffer)
                is Float -> judo.tabpage.vsplit(arg, newBuffer)
                is Double -> judo.tabpage.vsplit(arg.toFloat(), newBuffer)
                else -> throw IllegalArgumentException()
            }
        } }

        registerFn<Unit>(
            "unsplit",
            doc {
                usage {  }
                body { "Remove any split windows" }
            }
        ) { judo.tabpage.unsplit() }
    }

    private inline fun dispatchSplit(
        split: IJudoTabpage.(buffer: IJudoBuffer) -> IJudoWindow
    ): Any {
        val newBuffer = renderer.createBuffer()
        return engine.wrapWindow(
            judo.tabpage,
            judo.tabpage.split(newBuffer)
        )
    }

    /**
     * Adapt a suspending call into JudoCore as for the synchronous
     * scripting API.
     */
    private inline fun <R> adaptSuspend(crossinline block: suspend () -> R): R =
        // NOTE: we do not run on the JudoCore.dispatcher, since feedKey ensures
        // keys are processed there, and explicitly providing the dispatcher to
        // runBlocking can cause deadlocks
        runBlocking {
            block()
        }

    companion object {
        const val NAME = "cmd"
    }
}

