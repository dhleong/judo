package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRendererInfo
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.event.EventHandler
import net.dhleong.judo.event.handler
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.script.doc
import net.dhleong.judo.util.PatternSpec
import java.io.InputStream

/**
 * @author dhleong
 */
class CmdMode(
    judo: IJudoCore,
    private val ids: IdManager,
    inputBuffer: InputBuffer,
    rendererInfo: JudoRendererInfo,
    history: IInputHistory,
    private var completions: CompletionSource,
    private val engineFactory: ScriptingEngine.Factory
) : BaseCmdMode(judo, inputBuffer, rendererInfo, history) {

    private val engine by lazy {
        engineFactory.create().apply {
            init()
        }
    }

    override fun execute(code: String) {
        engine.execute(code)
    }

    override fun readFile(fileName: String, stream: InputStream) {
        engine.readFile(fileName, stream)
    }

    override fun callableToAliasProcessor(fromScript: Any): AliasProcesser =
        engine.callableToAliasProcessor(fromScript)

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
                    override fun invoke(arg: Any?) { fn1(arg!!) }
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

    private fun defineTrigger(trigger: PatternSpec, handler: Any) {
        queueTrigger(trigger.original)
        val fn = engine.callableToFunctionN(handler)
        judo.triggers.define(trigger) { args ->
            @Suppress("UNCHECKED_CAST")
            fn(args as Array<Any>)
        }
    }


    private fun ScriptingEngine.init() {
        initConsts()
        initCore()

        initAliases()
        initEvents()
        initKeymaps()
        initTriggers()
    }

    private fun ScriptingEngine.initConsts() {
        registerConst(
            "MYJUDORC",
            doc {
                body {
                    "Path to the Judo config file"
                }
            },
            USER_CONFIG_FILE.absolutePath
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

        registerFn<Unit>(
            "echo",
            doc {
                usage { withVarArgs() }
                body { "Print some output to the screen locally." }
            }
        ) { args: Array<Any> -> judo.echo(*args) }

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

    private fun ScriptingEngine.initTriggers() {
        registerFn<Unit>(
            "trigger",
            doc {
                usage(decorator = true) {
                    arg("inputSpec", "String/Pattern")
                    arg("options", "String", isOptional = true)
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
    }
}

