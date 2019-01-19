package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRenderer
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.event.EventHandler
import net.dhleong.judo.event.handler
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
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
    private val engineFactory: ScriptingEngine.Factory
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

    private fun definePrompt(prompt: PatternSpec, handler: Any) {
        queuePrompt(prompt.original)
        if (handler is String) {
            judo.prompts.define(prompt, handler)
        } else {
            val processor = callableToAliasProcessor(handler)
            judo.prompts.define(prompt, processor)
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

    private fun feedKeys(userInput: Array<Any>, mode: String) {
        val keys = userInput[0] as? String ?: throw IllegalArgumentException("[keys] must be a String")

        val remap =
            if (userInput.size == 1) true
            else userInput[1] as? Boolean ?: throw IllegalArgumentException("[remap] must be a Boolean")

        judo.feedKeys(keys, remap, mode)
    }

    private fun readInput(prompt: String): String? {
        // TODO user-provided completions?
        val inputMode = ScriptInputMode(judo, completions, prompt)
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
            toScript(judo)
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
        ) { args: Array<Any> ->
            if (args.isNotEmpty()) {
                readInput(args[0] as String)
            } else {
                readInput("")
            }
        }

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
        ) { args: Array<Any> -> feedKeys(args, mode = "normal") }

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
        registerFn(
            "connect",
            doc {
                usage {
                    arg("host", "String")
                    arg("port", "Int")
                }

                body { "Connect to a server." }
            },
            judo::connect
        )

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
                    usage { /* no args to list */ }

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
                    arg("inputSpec", "Pattern/String")
                    arg("options", "String", flags = PatternProcessingFlags::class.java)
                    arg("outputSpec", "String")
                }

                usage(decorator = true) {
                    arg("inputSpec", "Pattern/String")
                    arg("options", "String", flags = PatternProcessingFlags::class.java)
                    arg("handler", "Fn")
                }
                body { """
                    Prepare a prompt to be displayed in the status area.
                    See :help alias for more about `inputSpec`, and :help trigger for more
                    about the optional `options`.
                """.trimIndent() }
            }
        ) { args: Array<Any> ->
            if (args.size == 2) {
                definePrompt(compilePatternSpec(args[0], ""), args[1])
            } else {
                definePrompt(compilePatternSpec(args[0], args[1] as String), args[2])
            }
        }
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

                body {
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
        """.trimIndent() }
            }
        ) { arg: Any ->
            val newBuffer = renderer.createBuffer()
            engine.wrapWindow(
                judo.tabpage,
                if (arg is Int) judo.tabpage.hsplit(arg, newBuffer)
                else judo.tabpage.hsplit(arg as Float, newBuffer)
            )
        }

        registerFn<Unit>(
            "unsplit",
            doc {
                usage {  }
                body { "Remove any split windows" }
            }
        ) { judo.tabpage.unsplit() }
    }
}

