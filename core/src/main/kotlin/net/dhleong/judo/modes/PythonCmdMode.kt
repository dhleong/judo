package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRendererInfo
import net.dhleong.judo.StateMap
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.alias.IAliasManager
import net.dhleong.judo.alias.compileSimplePatternSpec
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.event.EventHandler
import net.dhleong.judo.event.IEventManager
import net.dhleong.judo.event.handler
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import net.dhleong.judo.logging.ILogManager
import net.dhleong.judo.mapping.IJudoMap
import net.dhleong.judo.mapping.IMapManagerPublic
import net.dhleong.judo.prompt.IPromptManager
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.render.IdManager
import net.dhleong.judo.render.JudoBuffer
import net.dhleong.judo.trigger.ITriggerManager
import net.dhleong.judo.util.PatternMatcher
import net.dhleong.judo.util.PatternProcessingFlags
import net.dhleong.judo.util.PatternSpec
import org.python.core.JyAttribute
import org.python.core.Py
import org.python.core.PyCallIter
import org.python.core.PyException
import org.python.core.PyFunction
import org.python.core.PyIterator
import org.python.core.PyModule
import org.python.core.PyObject
import org.python.core.PyObjectDerived
import org.python.core.PyStringMap
import org.python.core.PyType
import org.python.core.adapter.PyObjectAdapter
import org.python.modules.sre.MatchObject
import org.python.modules.sre.PatternObject
import org.python.util.PythonInterpreter
import java.io.File
import java.io.InputStream
import java.util.EnumSet
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

internal val PATTERN_MATCHING_FLAGS = setOf("color")

/**
 * Python-based Command mode
 *
 * @author dhleong
 */
class PythonCmdMode(
    judo: IJudoCore,
    private val ids: IdManager,
    inputBuffer: InputBuffer,
    rendererInfo: JudoRendererInfo,
    history: IInputHistory,
    private var completions: CompletionSource
) : BaseCmdMode(judo, inputBuffer, rendererInfo, history) {

    private val python = PythonInterpreter()
    private val keepModules = HashSet<String>()

    private val isPatternMatchingFlag: (String) -> Boolean =
        { maybeFlag -> maybeFlag.toLowerCase() in PATTERN_MATCHING_FLAGS }

    init {
        InterfaceAdapter.init()

        val globals = PyGlobals()

        // "constants" (don't know if we can actually make them constant)
        globals["MYJUDORC"] = USER_CONFIG_FILE.absolutePath

        // aliasing
        globals["alias"] = asMaybeDecorator<Any>(2) {
            defineAlias(compilePatternSpec(it[0], ""), it[1])
        }

        // events
        globals["event"] = asMaybeDecorator<Any>(takeArgs = 2) {
            defineEvent(it[0] as String, it[1] as PyFunction)
        }

        // prompts
        globals["prompt"] = asMaybeDecorator<Any>(3, minArgs = 1, isFlag = isPatternMatchingFlag) {
            if (it.size == 2) {
                definePrompt(compilePatternSpec(it[0], ""), it[1])
            } else {
                definePrompt(compilePatternSpec(it[0], it[1] as String), it[2])
            }
        }

        // triggers: trigger('input', <'flags',> fn)
        globals["trigger"] = asMaybeDecorator<Any>(3, minArgs = 1, isFlag = isPatternMatchingFlag) {
            if (it.size == 2) {
                defineTrigger(compilePatternSpec(it[0], ""), it[1] as PyFunction)
            } else {
                defineTrigger(compilePatternSpec(it[0], it[1] as String), it[2] as PyFunction)
            }
        }

        // map invocations
        sequenceOf(
            "" to "",
            "c" to "cmd",
            "i" to "insert",
            "n" to "normal"
        ).forEach { (letter, modeName) ->
            val minMapArgs =
                if (letter.isEmpty()) 1
                else 2
            globals["${letter}map"] = asUnitPyFn<Any>(2, minArgs = minMapArgs) {
                if (it.size == 1) {
                    // special case; map(mode)
                    judo.printMappings(it[0] as String)
                } else {
                    defineMap(modeName, it[0], it[1], true)
                }
            }
            globals["${letter}noremap"] = asUnitPyFn<Any>(2) {
                defineMap(modeName, it[0], it[1], false)
            }
            globals["${letter}unmap"] = asUnitPyFn<String>(1) {
                judo.unmap(modeName, it[0])
            }
        }

        globals["createMap"] = asUnitPyFn<Any>(4, minArgs = 3) {
            val remap =
                if (it.size == 4) it[3] as Boolean
                else false
            defineMap(it[0] as String, it[1] as String, it[2], remap)
        }
        globals["deleteMap"] = asUnitPyFn<String>(2) {
            judo.unmap(it[0], it[1])
        }

        globals["config"] = asUnitPyFn<Any>(2, minArgs = 0) { config(it) }
        globals["connect"] = asUnitPyFn<Any>(2) { judo.connect(it[0] as String, it[1] as Int) }
        globals["complete"] = asUnitPyFn<String>(1) { judo.seedCompletion(it[0]) }
        globals["createUserMode"] = asUnitPyFn<String>(1) { judo.createUserMode(it[0]) }
        globals["disconnect"] = asUnitPyFn<Any> { judo.disconnect() }
        globals["echo"] = asUnitPyFn<Any>(Int.MAX_VALUE) { judo.echo(*it) }
        globals["enterMode"] = asUnitPyFn<String>(1) { judo.enterMode(it[0]) }
        globals["exitMode"] = asUnitPyFn<Any> { judo.exitMode() }
        globals["hsplit"] = asPyFn<Any, PyObject>(1) {
            val newBuffer = JudoBuffer(ids)
            PyWindow(judo.tabpage,
                if (it[0] is Int) judo.tabpage.hsplit(it[0] as Int, newBuffer)
                else judo.tabpage.hsplit(it[0] as Float, newBuffer)
            )
        }
        globals["input"] = asPyFn<String, String?>(1, minArgs = 0) {
            if (it.isNotEmpty()) {
                readInput(it[0])
            } else {
                readInput("")
            }
        }
        globals["isConnected"] = asPyFn<Any, Boolean> { judo.isConnected() }
        globals["load"] = asUnitPyFn<String>(1) { load(it[0]) }
        globals["logToFile"] = asUnitPyFn<String>(2, minArgs = 1) {
            if (it.size == 1) logToFile(it[0])
            else logToFile(it[0], it[1])
        }
        globals["normal"] = asUnitPyFn<Any>(2, minArgs = 1) { feedKeys(it, mode = "normal") }
        globals["persistInput"] = asUnitPyFn<String>(1, minArgs = 0) {
            if (it.isNotEmpty()) {
                judo.persistInput(File(it[0]))
            } else {
                persistInput()
            }
        }
        globals["quit"] = asUnitPyFn<Any> { judo.quit() }
        globals["reconnect"] = asUnitPyFn<Any> { judo.reconnect() }
        globals["reload"] = asUnitPyFn<Any> { reload() }
        globals["send"] = asUnitPyFn<String>(1) { judo.send(it[0], true) }
        globals["startInsert"] = asUnitPyFn<Any> { judo.enterMode("insert") }
        globals["stopInsert"] = asUnitPyFn<Any> { judo.exitMode() }
        globals["unalias"] = asUnitPyFn<String>(1) { judo.aliases.undefine(it[0]) }
        globals["unsplit"] = asUnitPyFn<Any> { judo.tabpage.unsplit() }
        globals["untrigger"] = asUnitPyFn<String>(1) { judo.triggers.undefine(it[0]) }

        globals["judo"] = judo

        // the naming here is insane, but correct
        python.locals = globals

        // also, add as a module
        val asModule = PyModule("judo", globals)
        val modules = python.systemState.modules as PyStringMap
        modules.__setitem__("judo", asModule)

        // don't override our input()!!
        val builtins = modules.__getitem__("__builtin__")
        builtins.dict.__setitem__("input", globals.__getitem__("input"))

        modules.keys().asIterable().forEach {
            keepModules.add(it.asString())
        }
    }

    private fun defineAlias(alias: PatternSpec, handler: Any) {
        queueAlias(alias.original)
        if (handler is PyFunction) {
            val handlerFn: AliasProcesser = { args ->
                wrapExceptions {
                    handler.__call__(args.map { Py.java2py(it) }.toTypedArray())
                           .__tojava__(String::class.java)
                        as String?
                        ?: ""
                }
            }

            judo.aliases.define(alias, handlerFn)
        } else {
            judo.aliases.define(alias, handler as String)
        }
    }

    private fun defineEvent(eventName: String, pyHandler: PyFunction) {
        val argCount = pyHandler.__code__.__getattr__("co_argcount").asInt()
        val handler: EventHandler = when (argCount) {
            0 -> handler { wrapExceptions { pyHandler.__call__() } }
            1 -> { arg -> wrapExceptions { pyHandler.__call__(Py.java2py(arg)) } }
            else -> { rawArg ->
                if (rawArg !is Array<*>) {
                    throw ScriptExecutionException(
                        "$pyHandler expected $argCount arguments, but event arg was not an array")
                }

                val pythonArgs = Array<PyObject>(rawArg.size) { index ->
                    Py.java2py(rawArg[index])
                }
                wrapExceptions {
                    pyHandler.__call__(pythonArgs)
                }
            }
        }
        judo.events.register(eventName, handler)
        queueEvent(eventName, handler)
    }

    private fun defineMap(modeName: String, fromKeys: Any, mapTo: Any, remap: Boolean) {
        if (mapTo is String) {
            judo.map(
                modeName,
                fromKeys as String,
                mapTo,
                remap)
        } else if (mapTo is PyFunction) {
            judo.map(
                modeName,
                fromKeys as String,
                {
                    wrapExceptions {
                        mapTo.__call__()
                    }
                },
                mapTo.toString()
            )
        } else {
            throw IllegalArgumentException("Unexpected map-to value")
        }

        queueMap(modeName, fromKeys)
    }

    private fun definePrompt(alias: PatternSpec, handler: Any) {
        queuePrompt(alias.original)
        if (handler is PyFunction) {
            judo.prompts.define(alias, { args ->
                wrapExceptions {
                    handler.__call__(args.map { Py.java2py(it) }.toTypedArray())
                        .__tojava__(String::class.java)
                        as String
                }
            })
        } else {
            judo.prompts.define(alias, handler as String)
        }
    }

    private fun defineTrigger(alias: PatternSpec, handler: PyFunction) {
        queueTrigger(alias.original)
        judo.triggers.define(alias, { args ->
            wrapExceptions {
                handler.__call__(args.map { Py.java2py(it) }.toTypedArray())
            }
        })
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

    override fun execute(code: String) {
        wrapExceptions(lineExecution = true) {
            python.exec(code)
        }
    }

    override fun reload() {
        // clean up modules
        val modules = python.systemState.modules as PyStringMap
        val toRemove = HashSet<PyObject>()
        modules.keys().asIterable()
            .filter { it.asString() !in keepModules }
            .forEach {
                toRemove.add(it)
            }

        for (keyToRemove in toRemove) {
            modules.__delitem__(keyToRemove)
        }

        super.reload()
    }

    override fun readFile(file: File, inputStream: InputStream) {
        file.parentFile?.let { fileDir ->
            python.exec(
                """
                import sys
                sys.path.insert(0, '${fileDir.absolutePath}')
                """.trimIndent())
        }
        super.readFile(file, inputStream)
    }

    override fun readFile(fileName: String, stream: InputStream) {
        wrapExceptions {
            python.execfile(stream, fileName)
        }
    }

    private inline fun <R> wrapExceptions(lineExecution: Boolean = false, block: () -> R): R {
        try {
            return block()
        } catch (e: PyException) {
            if (lineExecution) {
                // if the single line execution's cause was a ScriptExecution,
                // that's the only info we need
                val cause = e.cause
                if (cause is ScriptExecutionException) {
                    throw cause
                }
            }

            throw ScriptExecutionException(e.toString())
        }
    }
}

internal fun PyWindow(tabpage: IJudoTabpage, window: IJudoWindow): PyObject {
    val resize = asUnitPyFn<Int>(2) {
        window.resize(it[0], it[1])
        tabpage.resize()
    }

    return object : PyObject() {
        override fun __findattr_ex__(name: String?): PyObject? =
            when (name ?: "") {
                "buffer" -> PyBuffer(window, window.currentBuffer) // cache?
                "height" -> Py.java2py(window.height)
                "width" -> Py.java2py(window.width)
                "id" -> Py.java2py(window.id)

                "close" -> asUnitPyFn<Any> { tabpage.close(window) } // not used oft enough to cache
                "resize" -> resize

                else -> super.__findattr_ex__(name)
            }
    }
}

internal fun PyBuffer(window: IJudoWindow, buffer: IJudoBuffer): PyObject {
    val append = asUnitPyFn<String>(1) {
        buffer.appendLine(it[0], false, window.width, false)
    }
    val clear = asUnitPyFn<Any> {
        buffer.clear()
    }
    val set = asUnitPyFn<List<String>>(1) {
        buffer.set(it[0])
    }

    return object : PyObject() {
        override fun __len__(): Int {
            return buffer.size
        }

        override fun __findattr_ex__(name: String?): PyObject =
            when (name ?: "") {
                "append" -> append
                "clear" -> clear
                "set" -> set
                "id" -> Py.java2py(buffer.id)

                else -> super.__findattr_ex__(name)
            }
    }
}

inline private fun <reified T: Any> asMaybeDecorator(
    takeArgs: Int,
    minArgs: Int = takeArgs - 1,
    crossinline fn: (Array<T>) -> Unit): PyObject =
    asMaybeDecorator(takeArgs, minArgs,
        acceptsFlag = false,
        isFlag = { false },
        fn = fn
    )

inline private fun <reified T: Any> asMaybeDecorator(
    takeArgs: Int,
    minArgs: Int = takeArgs - 1,
    crossinline isFlag: (String) -> Boolean,
    crossinline fn: (Array<T>) -> Unit): PyObject =
    asMaybeDecorator(takeArgs, minArgs,
        acceptsFlag = true,
        isFlag = isFlag,
        fn = fn
    )

/**
 * Create a Python function that can be used either as a normal
 * function OR a decorator
 */
inline private fun <reified T: Any> asMaybeDecorator(
        takeArgs: Int,
        minArgs: Int = takeArgs - 1,
        acceptsFlag: Boolean,
        crossinline isFlag: (String) -> Boolean,
        crossinline fn: (Array<T>) -> Unit): PyObject {
    return asPyFn<T, PyObject?>(takeArgs, minArgs) { args ->
        if (isDecoratorCall(args, minArgs, takeArgs, acceptsFlag, isFlag)) {
            // decorator mode; we return a function that accepts
            // a function and finally calls `fn`
            asPyFn<PyObject, PyObject>(1) { wrappedArgs ->
                val combined = args + (wrappedArgs[0] as T)
                fn(combined)
                wrappedArgs[0]
            }
        } else {
            // regular function call
            fn(args)
            null
        }
    }
}

private inline fun <T : Any> isDecoratorCall(
    args: Array<T>, minArgs: Int, takeArgs: Int,
    acceptsFlag: Boolean, isFlag: (String) -> Boolean
): Boolean {
    if (args.size !in minArgs..(takeArgs - 1)) return false

    val lastArg = args.last()
    if (lastArg is PyFunction) return false

    if (acceptsFlag) {
        if (lastArg is String && isFlag(lastArg)) {
            return true
        }

        if (args.size == takeArgs - 1
                && lastArg is String
                && !isFlag(lastArg)) {
            // one less than takeArgs, but last arg is not a flag.
            // This must be a regular function call
            return false
        }
    }

    // otherwise, it's a decorator!
    return true
}

inline private fun <reified T: Any> asUnitPyFn(
        takeArgs: Int = 0,
        minArgs: Int = takeArgs,
        crossinline fn: (Array<T>) -> Unit): PyObject {
    return asPyFn(takeArgs, minArgs, fn)
}

inline private fun <reified T: Any, reified R> asPyFn(
        takeArgs: Int = 0,
        minArgs: Int = takeArgs,
        crossinline fn: (Array<T>) -> R): PyObject {
    return object : PyObject() {
        override fun __call__(args: Array<PyObject>, keywords: Array<String>): PyObject {
            if (minArgs != Int.MAX_VALUE && args.size < minArgs) {
                throw IllegalArgumentException("Expected $minArgs arguments; got ${args.size}")
            }

            val typedArgs =
                if (takeArgs == 0) emptyArray()
                else {
                    args.take(takeArgs)
                        .map<PyObject, T> { T::class.java.cast(it.__tojava__(T::class.java)) }
                        .toTypedArray()
                }

            val result = fn(typedArgs)
            if (T::class == Unit::class) {
                return Py.None
            }

            return Py.java2py(result)
        }
    }
}

private class PyGlobals : PyStringMap() {

    private val reservedSet = HashSet<String>()

    override fun __setitem__(key: String?, value: PyObject?) {
        if (key !in reservedSet) {
            super.__setitem__(key, value)
        }
    }

    operator fun set(key: String, value: PyObject) {
        reservedSet.add(key)
        super.__setitem__(key, value)
    }

    operator fun set(key: String, value: Any) {
        reservedSet.add(key)
        super.__setitem__(key, Py.java2py(value))
    }
}

internal fun compilePatternSpec(input: Any, flags: String): PatternSpec {
    val flagsSet: EnumSet<PatternProcessingFlags>
    if (flags.isNotBlank()) {
        flagsSet = EnumSet.copyOf(PatternProcessingFlags.NONE)
        if (flags.contains("color", ignoreCase = true)) {
            flagsSet.add(PatternProcessingFlags.KEEP_COLOR)
        }
    } else {
        flagsSet = PatternProcessingFlags.NONE
    }

    return when (input) {
        is String -> compileSimplePatternSpec(input, flagsSet)
        is PatternObject -> {
            // first, try to compile it as a Java regex Pattern;
            // that will be much more efficient than delegating
            // to Python regex stuff (since we have to allocate
            // arrays for just about every call)
            val patternAsString = input.pattern.string
            try {
                val javaPattern = Pattern.compile(patternAsString)

                // if we got here, huzzah! no compile issues
                CoercedRegexAliasSpec(
                    patternAsString,
                    javaPattern,
                    input.groups,
                    flagsSet
                )
            } catch (e: PatternSyntaxException) {
                // alas, fallback to using the python pattern
                PyPatternSpec(input, flagsSet)
            }
        }

        else -> throw IllegalArgumentException(
            "Invalid alias type: $input (${input.javaClass})")
    }
}


internal class CoercedRegexAliasSpec(
    override val original: String,
    private val pattern: Pattern,
    override val groups: Int,
    override val flags: EnumSet<PatternProcessingFlags>
) : PatternSpec {
    override fun matcher(input: CharSequence): PatternMatcher =
        CoercedRegexAliasMatcher(pattern.matcher(input))
}

internal class CoercedRegexAliasMatcher(
    private val matcher: Matcher
) : PatternMatcher {
    override fun find(): Boolean = matcher.find()

    override fun group(index: Int): String =
        // NOTE group 0 is the entire pattern
        matcher.group(index + 1)

    override val start: Int
        get() = matcher.start()
    override fun start(index: Int): Int =
        matcher.start(index + 1)

    override val end: Int
        get() = matcher.end()
    override fun end(index: Int): Int =
        matcher.end(index + 1)
}


/**
 * If we can't use the Python pattern as a Java pattern,
 *  we have to fall back to this
 */
internal class PyPatternSpec(
    private val pattern: PatternObject,
    override val flags: EnumSet<PatternProcessingFlags>
) : PatternSpec {
    override val groups: Int = pattern.groups

    override fun matcher(input: CharSequence): PatternMatcher =
        PyPatternMatcher(
            pattern.finditer(arrayOf(Py.java2py(input.toString())), emptyArray())
                as PyCallIter
        )

    override val original: String = pattern.pattern.string
}

internal class PyPatternMatcher(finditer: PyIterator) : PatternMatcher {

    private var iterator = finditer.iterator()
    private var current: MatchObject? = null

    override fun find(): Boolean =
        if (iterator.hasNext()) {
            current = iterator.next() as MatchObject
            true
        } else {
            current = null
            false
        }

    override fun group(index: Int): String =
        // NOTE: group 0 means everything that matched, but
        // index 0 means that actual matching group; so, index + 1
        current!!.group(arrayOf(Py.java2py(index + 1))).asString()

    override val start: Int
        get() = current!!.start().asInt()

    override fun start(index: Int): Int =
        current!!.start(Py.java2py(index + 1)).asInt()

    override val end: Int
        get() = current!!.end().asInt()

    override fun end(index: Int): Int =
        current!!.end(Py.java2py(index + 1)).asInt()
}

internal fun <T> java2py(type: Class<T>, obj: T): PyObject {
    val pyObj = PyObjectDerived(PyType.fromClass(type, false))
    JyAttribute.setAttr(pyObj, JyAttribute.JAVA_PROXY_ATTR, obj)
    return pyObj
}

class InterfaceAdapter : PyObjectAdapter {
    companion object {
        private var isInitialized = false

        internal val exposedInterfaces = arrayOf(
            IJudoCore::class.java,

            IAliasManager::class.java,
            IEventManager::class.java,
            ILogManager::class.java,
            IMapManagerPublic::class.java,
            IPromptManager::class.java,
            ITriggerManager::class.java,
            StateMap::class.java,
            JudoRendererInfo::class.java,
            IJudoTabpage::class.java,

            IJudoMap::class.java
        )

        fun init() {
            if (!isInitialized) {
                Py.getAdapter().addPostClass(InterfaceAdapter())
            }
        }
    }

    override fun adapt(obj: Any?): PyObject {
        val type = findInterfaceFor(obj)!!
        val pyObj = PyObjectDerived(PyType.fromClass(type, false))
        JyAttribute.setAttr(pyObj, JyAttribute.JAVA_PROXY_ATTR, obj)
        return pyObj
    }

    override fun canAdapt(obj: Any?): Boolean {
        if (obj == null) return false
        return findInterfaceFor(obj) != null
    }

    fun findInterfaceFor(obj: Any?): Class<*>? {
        if (obj == null) return null
        return InterfaceAdapter.exposedInterfaces.firstOrNull {
            it.isInstance(obj)
        }
    }
}
