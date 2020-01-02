package net.dhleong.judo.modes

import kotlinx.coroutines.withContext
import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRenderer
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.event.EventHandler
import net.dhleong.judo.event.handler
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.script.JudoScriptingEntity
import net.dhleong.judo.script.ScriptInitContext
import net.dhleong.judo.script.ScriptingEngine
import net.dhleong.judo.script.init.initAliases
import net.dhleong.judo.script.init.initConnection
import net.dhleong.judo.script.init.initConsts
import net.dhleong.judo.script.init.initCore
import net.dhleong.judo.script.init.initEvents
import net.dhleong.judo.script.init.initFiles
import net.dhleong.judo.script.init.initKeymaps
import net.dhleong.judo.script.init.initModes
import net.dhleong.judo.script.init.initMultiTriggers
import net.dhleong.judo.script.init.initPrompts
import net.dhleong.judo.script.init.initTriggers
import net.dhleong.judo.script.init.initUtil
import net.dhleong.judo.script.init.initWindows
import net.dhleong.judo.util.PatternSpec
import java.io.File
import java.io.InputStream

/**
 * @author dhleong
 */
class CmdMode(
    judo: IJudoCore,
    inputBuffer: InputBuffer,
    renderer: JudoRenderer,
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

    private var currentScriptFile: File? = null

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

    private fun ScriptingEngine.init() {
        val context = ScriptInitContext(
            judo, this,
            userConfigFile, this@CmdMode,
            completionSource, myRegisteredFns, myRegisteredVars
        )
        with(context) {
            initConsts()
            initCore()

            initAliases()
            initConnection()
            initEvents()
            initFiles()
            initKeymaps()
            initModes()
            initMultiTriggers()
            initPrompts()
            initTriggers()
            initUtil()
            initWindows()
        }
    }

    fun interrupt() {
        engine.interrupt()
    }

    override suspend fun execute(code: String) {
        engine.execute(code)
    }

    override suspend fun executeImplicit(fnName: String) {
        engine.execute(engineFactory.formatFnCall(fnName))
    }

    override fun readFile(file: File, inputStream: InputStream) = withCurrentScriptFile(file) {
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

    internal fun createMap(modeName: String, fromKeys: String, mapTo: Any, remap: Boolean) {
        when (mapTo) {
            is String -> judo.map(
                modeName,
                fromKeys,
                mapTo,
                remap
            )

            else -> {
                val fn = try {
                    callableToFunction0<Unit>(mapTo)
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
                    { withContext(dispatcher) {
                        fn()
                    } },
                    mapTo.toString()
                )
            }
        }

        queueMap(modeName, fromKeys)
    }

    private fun <R> callableToFunction0(fromScript: Any): () -> R =
        dispatcher.wrapWithLock(engine.callableToFunction0(fromScript))

    private fun callableToFunction1(fromScript: Any): Function1<Any?, Any?> {
        val fn = engine.callableToFunction1(fromScript)
        return { arg ->
            dispatcher.withLockBlocking {
                fn(arg)
            }
        }
    }

    private fun callableToFunctionN(fromScript: Any): Function1<Array<Any?>, Any?> {
        val fn = engine.callableToFunctionN(fromScript)
        return { arg ->
            dispatcher.withLockBlocking {
                fn(arg)
            }
        }
    }

    internal fun defineEvent(eventName: String, handlerFromScript: Any) {
        val handler: EventHandler = when (val argCount = engine.callableArgsCount(handlerFromScript)) {
            0 -> {
                val fn0 = callableToFunction0<Any>(handlerFromScript)
                handler { fn0() }
            }

            1 -> {
                val fn1 = callableToFunction1(handlerFromScript)
                object : EventHandler {
                    override fun invoke(arg: Any?) { fn1(arg) }
                }
            }

            else -> {
                val fnN = callableToFunctionN(handlerFromScript)

                object : EventHandler {
                    override fun invoke(rawArg: Any?) {
                        if (rawArg !is Array<*>) {
                            throw ScriptExecutionException(
                                "$handlerFromScript expected $argCount arguments, but event arg was not an array")
                        }

                        @Suppress("UNCHECKED_CAST")
                        fnN(rawArg as Array<Any?>)
                    }
                }
            }
        }

        judo.events.register(eventName, handler)
        queueEvent(eventName, handler)
    }

    /** User-input group number */
    internal fun tryDefinePrompt(group: Int, prompt: PatternSpec, handler: Any) {
        if (group <= 0) {
            throw IllegalArgumentException("group must be > 0")
        }
        definePrompt(group, prompt, handler)
    }
    internal fun definePrompt(group: Int, prompt: PatternSpec, handler: Any) {
        queuePrompt(prompt.original)
        if (handler is String) {
            judo.prompts.define(prompt, handler, group)
        } else {
            val processor = callableToAliasProcessor(handler)
            judo.prompts.define(prompt, processor, group)
        }
    }

    internal fun defineTrigger(trigger: PatternSpec, handler: Any) {
        queueTrigger(trigger.original)
        val fn = engine.callableToFunctionN(handler)
        judo.triggers.define(trigger) { args ->
            @Suppress("UNCHECKED_CAST")
            fn(args as Array<Any?>)
        }
    }

    internal suspend fun feedKeys(userInput: Array<Any>, mode: String) {
        val keys = userInput[0] as? String ?: throw IllegalArgumentException("[keys] must be a String")

        val remap =
            if (userInput.size == 1) true
            else userInput[1] as? Boolean ?: throw IllegalArgumentException("[remap] must be a Boolean")

        judo.feedKeys(keys, remap, mode)
    }

    internal suspend fun readInput(prompt: String): String? {
        // TODO user-provided completions?
        val inputMode = ScriptInputMode(
            judo, completions,
            inputCmdBuffer, inputCmdHistory,
            prompt
        )
        judo.enterMode(inputMode)
        return inputMode.awaitResult()
    }

    internal fun expandPath(type: String): String? = when (type) {
        "<init>" -> userConfigFile.absolutePath
        "<lastread>" -> lastReadFile?.absolutePath
        "<sfile>" -> currentScriptFile?.absolutePath

        // TODO: current world URI?

        else -> null
    }

    private inline fun withCurrentScriptFile(file: File, block: () -> Unit) {
        val old = currentScriptFile
        currentScriptFile = file
        try {
            block()
        } finally {
            currentScriptFile = old
        }
    }

    companion object {
        const val NAME = "cmd"
    }
}
