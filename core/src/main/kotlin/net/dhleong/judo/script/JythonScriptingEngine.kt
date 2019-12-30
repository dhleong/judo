package net.dhleong.judo.script

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRendererInfo
import net.dhleong.judo.StateMap
import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.alias.IAliasManager
import net.dhleong.judo.alias.compileSimplePatternSpec
import net.dhleong.judo.event.IEventManager
import net.dhleong.judo.logging.ILogManager
import net.dhleong.judo.mapping.IJudoMap
import net.dhleong.judo.mapping.IMapManagerPublic
import net.dhleong.judo.modes.ScriptExecutionException
import net.dhleong.judo.prompt.IPromptManager
import net.dhleong.judo.render.IJudoBuffer
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.trigger.ITriggerManager
import net.dhleong.judo.util.PatternMatcher
import net.dhleong.judo.util.PatternProcessingFlags
import net.dhleong.judo.util.PatternSpec
import org.python.core.JyAttribute
import org.python.core.Py
import org.python.core.PyCallIter
import org.python.core.PyException
import org.python.core.PyFrame
import org.python.core.PyFunction
import org.python.core.PyInteger
import org.python.core.PyIterator
import org.python.core.PyModule
import org.python.core.PyNone
import org.python.core.PyObject
import org.python.core.PyObjectDerived
import org.python.core.PyStringMap
import org.python.core.PyTuple
import org.python.core.PyType
import org.python.core.StdoutWrapper
import org.python.core.TraceFunction
import org.python.core.adapter.PyObjectAdapter
import org.python.modules.sre.MatchObject
import org.python.modules.sre.PatternObject
import org.python.util.PythonInterpreter
import java.io.File
import java.io.InputStream
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

private const val INTERRUPTED = Int.MIN_VALUE

/**
 * @author dhleong
 */
class JythonScriptingEngine : ScriptingEngine {
    class Factory : ScriptingEngine.Factory {
        override val supportsDecorators: Boolean
            get() = true

        override fun supportsFileType(ext: String): Boolean = ext == "py"

        override fun create(): ScriptingEngine = JythonScriptingEngine()

        override fun toString(): String = "PY ScriptingFactory (Jython)"
    }

    private val python = PythonInterpreter()
    private val keepModules = HashSet<String>()
    private val globals = PyGlobals()

    private val executing = AtomicInteger(0)

    private lateinit var printFn: (Array<Any?>) -> Unit

    init {
        InterfaceAdapter.init()

        // the naming here is insane, but correct
        python.locals = globals
    }

    override fun onPostRegister() {
        // add everything as a module
        val asModule = PyModule("judo", globals)
        val modules = python.systemState.modules as PyStringMap
        modules.__setitem__("judo", asModule)

        // don't override our input() or print()!!
        val builtins = modules.__getitem__("__builtin__")
        builtins.dict.__setitem__("input", globals.__getitem__("input"))
        builtins.dict.__setitem__("print", globals.__getitem__("print"))

        // the above doesn't seem to do it for print(), so we just wrap stdout
        val out = object : StdoutWrapper() {
            override fun myFile(): PyObject? = null

            override fun flush() { /* nop */ }
            override fun flushLine() { /* nop */ }

            override fun write(s: String?) {
                printFn(arrayOf(s))
            }

            override fun println(o: PyObject?) {
                if (o == null) {
                    return printFn(emptyArray())
                }

                printFn(when (o) {
                    is PyTuple -> o.map { maybeToJava(it) }.toTypedArray()

                    else -> arrayOf(maybeToJava(o))
                })
            }

            override fun println(o: String?) {
                printFn(arrayOf(o))
            }

            override fun print(args: Array<out PyObject>?, sep: PyObject?, end: PyObject?) {
                if (args == null) {
                    printFn(emptyArray())
                    return
                }

                printFn(Array(args.size) { toJava(it) })
            }
        }
        python.setOut(out)

        // hacks because the global StdoutWrapper converts everything to a string
        // before passing it to the one set above
        Py::class.java.getDeclaredField("stdout").apply {
            isAccessible = true
            set(null, out)
        }

        modules.keys().asIterable().forEach {
            keepModules.add(it.asString())
        }

        Py.getThreadState().tracefunc = object : TraceFunction() {
            override fun traceReturn(p0: PyFrame?, p1: PyObject?): TraceFunction = this

            override fun traceLine(p0: PyFrame?, p1: Int): TraceFunction = this.also {
                if (executing.compareAndSet(INTERRUPTED, 0)) {
                    throw InterruptedException()
                }
            }

            override fun traceException(p0: PyFrame?, p1: PyException?): TraceFunction = this

            override fun traceCall(p0: PyFrame?): TraceFunction = this
        }
    }

    override fun register(entity: JudoScriptingEntity) {
        when (entity) {
            is JudoScriptingEntity.Constant<*> -> {
                globals[entity.name] = entity.value!!
            }

            is JudoScriptingEntity.Function<*> -> {
                if (entity.name == "print") {
                    @Suppress("UNCHECKED_CAST")
                    printFn = entity.fn as (Array<Any?>) -> Unit
                }
                globals[entity.name] = entity.toPyFn()
            }
        }
    }

    override fun interrupt() {
//        org.python.modules.thread.thread.interruptAllThreads()
//        pythonThread.interrupt()
        if (executing.get() > 0) {
            executing.set(INTERRUPTED)
        }
    }

    override fun execute(code: String) {
        wrapExceptions(lineExecution = true) {
            python.exec(code)
        }
    }

    override fun readFile(fileName: String, stream: InputStream) {
        wrapExceptions {
            python.execfile(stream, fileName)
        }
    }

    override fun toScript(fromJava: Any): Any = Py.java2py(fromJava)
    override fun toJava(fromScript: Any): Any =
        (fromScript as? PyObject)?.__tojava__(Any::class.java)
            ?: fromScript

    private fun maybeToJava(fromScript: Any?) = fromScript?.let { toJava(it) }

    override fun callableArgsCount(fromScript: Any): Int {
        val pyHandler = fromScript as PyFunction
        return pyHandler.__code__.__getattr__("co_argcount").asInt()
    }

    override fun callableToAliasProcessor(fromScript: Any): AliasProcesser {
        val handler = fromScript as? PyFunction ?: throw IllegalArgumentException()
        return { args ->
            wrapExceptions {
                handler.__call__(args.map { Py.java2py(it) }.toTypedArray())
                    .__tojava__(String::class.java)
                    as String?
            }
        }
    }

    override fun <R> callableToFunction0(fromScript: Any): () -> R = fromScript.asFn { fn -> {
        wrapExceptions {
            @Suppress("UNCHECKED_CAST")
            fn.__call__() as R
        }
    } }

    override fun callableToFunction1(fromScript: Any): (Any?) -> Any? = fromScript.asFn { fn ->
        { arg -> wrapExceptions {
            fn.__call__(Py.java2py(arg))
        } }
    }

    override fun callableToFunctionN(fromScript: Any): (Array<Any?>) -> Any? = fromScript.asFn { fn ->
        { rawArg -> wrapExceptions {
            val pythonArgs = Array<PyObject>(rawArg.size) { index ->
                Py.java2py(rawArg[index])
            }
            fn.__call__(pythonArgs)
        } }
    }

    private inline fun <R> Any.asFn(block: (PyFunction) -> R): R {
        val fn = this as? PyFunction ?: throw IllegalArgumentException("$this is not a Fn")
        return block(fn)
    }

    override fun compilePatternSpec(fromScript: Any, flags: String): PatternSpec {
        val flagsSet = patternSpecFlagsToEnums(flags)

        return when (fromScript) {
            is String -> compileSimplePatternSpec(fromScript, flagsSet)
            is PatternObject -> {
                // first, try to compile it as a Java regex Pattern;
                // that will be much more efficient than delegating
                // to Python regex stuff (since we have to allocate
                // arrays for just about every call)
                val patternAsString = fromScript.pattern.string
                try {
                    val javaPattern = Pattern.compile(patternAsString)

                    // if we got here, huzzah! no compile issues
                    JavaRegexPatternSpec(
                        patternAsString,
                        javaPattern,
                        flagsSet
                    )
                } catch (e: PatternSyntaxException) {
                    // alas, fallback to using the python pattern
                    PyPatternSpec(fromScript, flagsSet)
                }
            }

            else -> throw IllegalArgumentException(
                "Invalid alias type: $fromScript (${fromScript.javaClass})")
        }
    }

    override fun wrapCore(judo: IJudoCore) = createPyCore(this, judo)

    override fun wrapWindow(
        tabpage: IJudoTabpage,
        window: IJudoWindow
    ) = createPyWindow(this, tabpage, window)

    override fun onPreReadFile(file: File, inputStream: InputStream) {
        file.parentFile?.let { fileDir ->
            python.exec(
                """
                import sys
                sys.path.insert(0, '${fileDir.absolutePath}')
                """.trimIndent())
        }
    }

    override fun onPreReload() {
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

        super.onPreReload()
    }

    private inline fun <R> wrapExceptions(lineExecution: Boolean = false, block: () -> R): R {
        try {
            executing.incrementAndGet()
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

            val interruptedClassName = InterruptedException::class.java.canonicalName
            val string = e.toString()
            val interruptedIndex = string.indexOf(interruptedClassName)
            val message = if (interruptedIndex == -1) string
                else string.substring(0, interruptedIndex + interruptedClassName.length)
            throw ScriptExecutionException(message, e.cause)
        } finally {
            executing.decrementAndGet()
        }
    }

    private fun <R> JudoScriptingEntity.Function<R>.toPyFn(): PyObject {
        val usages = doc.invocations ?: throw IllegalArgumentException("No invocations for Fn?")
        val hasVarArgs = usages.any { it.hasVarArgs }
        val hasDecorator = usages.any { it.canBeDecorator }
        val flagsType = usages.flatMap { usage ->
            usage.args.map { it.flags }
        }.firstOrNull { it != null }
        val maxArgs = when {
            hasVarArgs -> Int.MAX_VALUE
            else -> usages.maxBy { it.args.size }!!.args.size
        }
        val minArgs = usages.asSequence().map { usage ->
            usage.args.sumBy {
                if (it.isOptional) 0
                else 1
            }
        }.min() ?: throw IllegalStateException()
        val callable = this.toFunctionalInterface(this@JythonScriptingEngine)

        return if (hasDecorator) {
            asMaybeDecorator<Any>(
                name,
                usages.first { it.canBeDecorator },
                isFlag = flagCheckerFor(flagsType),
                takeArgs = maxArgs,
                minArgs = minArgs - 1 // as a decorator, it can be called with min-1
            ) { args -> callable.call(*args) }
        } else {
            asPyFn<Any, Any?>(
                name,
                takeArgs = maxArgs,
                minArgs = minArgs
            ) { args -> callable.call(*args) }
        }
    }
}

@Suppress("NOTHING_TO_INLINE", "RedundantLambdaArrow")
private inline fun flagCheckerFor(type: Class<out Enum<*>>?): (String) -> Boolean {
    return if (type == null) { _ -> false }
    else {
        val stringTypes = type.declaredFields.filter {
            it.isEnumConstant
        }.map {
            it.get(type).toString().toLowerCase()
        }.toSet()

        return { input -> stringTypes.contains(input.toLowerCase()) }
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

/**
 * Create a Python function that can be used either as a normal
 * function OR a decorator
 */
private inline fun <reified T: Any> asMaybeDecorator(
    name: String,
    decoratorUsage: JudoScriptInvocation,
    takeArgs: Int,
    minArgs: Int = takeArgs - 1,
    crossinline isFlag: (String) -> Boolean,
    crossinline fn: (Array<T>) -> Unit
) = asPyFn<T, PyObject?>(name, takeArgs, minArgs) { args ->
    if (isDecoratorCall(decoratorUsage, args, minArgs, takeArgs, isFlag)) {
        // decorator mode; we return a function that accepts
        // a function and finally calls `fn`
        asPyFn<PyObject, PyObject>(name, 1) { wrappedArgs ->
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

private inline fun <T : Any> isDecoratorCall(
    decoratorUsage: JudoScriptInvocation,
    args: Array<T>, minArgs: Int, takeArgs: Int,
    isFlag: (String) -> Boolean
): Boolean {
    if (args.size !in minArgs until takeArgs) return false

    val lastArg = args.last()
    if (lastArg is PyFunction) return false

    var argsToAccountFor = args.size

    var index = 0
    for (arg in decoratorUsage.args) {
        // probably, got to the handler without seeing a flag
        if (index > args.lastIndex) break

        // NOTE: check flags first, because they are also optional!
        when {
            arg.flags != null ->
                if (args[index] is String && isFlag(args[index] as String)) {
                    // we saw the flag! if this is the last arg provided,
                    // it's definitely a decorator invocation;
                    // if not, it's definitely *not*
                    return index == args.lastIndex
                }

            arg.isOptional ->
                if (arg.typeMatches(args[index])) {
                    // optional arg provided; move on
                    ++index
                    --argsToAccountFor
                }

            // required arg
            else -> ++index
        }
    }

    return argsToAccountFor == minArgs
}


private inline fun <reified T: Any, reified R> asPyFn(
    name: String,
    takeArgs: Int = 0,
    minArgs: Int = takeArgs,
    crossinline fn: (Array<T>) -> R
): PyObject = object : PyObject() {
    override fun __call__(args: Array<PyObject>, keywords: Array<String>): PyObject {
        if (minArgs != Int.MAX_VALUE && args.size < minArgs) {
            throw IllegalArgumentException("$name Expected $minArgs arguments; got ${args.size}")
        }

        if (args.size > takeArgs) {
            throw IllegalArgumentException("$name Expected no more than $takeArgs arguments; got ${args.size}")
        }

        val typedArgs =
            if (takeArgs == 0) emptyArray()
            else {
                args.take(takeArgs)
                    .map<PyObject, T?> { T::class.java.cast(it.__tojava__(T::class.java)) }
                    .toTypedArray()
            }

        @Suppress("UNCHECKED_CAST")
        val result = fn(typedArgs as Array<T>)
        if (T::class == Unit::class || result == null) {
            return Py.None
        }

        return Py.java2py(result)
    }
}

internal fun createPyCore(
    engine: JythonScriptingEngine,
    judo: IJudoCore
): PyObject {
    val currentObjects = object : PyObject() {
        override fun __findattr_ex__(name: String?): PyObject? =
            when (name ?: "") {
                "tabpage" -> createPyTabpage(judo.renderer.currentTabpage)
                "window" -> createPyWindow(
                    engine,
                    judo.renderer.currentTabpage,
                    judo.renderer.currentTabpage.currentWindow
                )
                "buffer" -> createPyBuffer(
                    judo.renderer.currentTabpage.currentWindow,
                    judo.renderer.currentTabpage.currentWindow.currentBuffer
                )
                else -> super.__findattr_ex__(name)
            }

        override fun __setattr__(name: String?, value: PyObject?) {
            when (name ?: "") {
                "tabpage" -> {
                    judo.renderer.currentTabpage =
                        value?.toJava<IJudoTabpage>()
                        ?: throw IllegalArgumentException()
                }
                "window" -> {
                    judo.renderer.currentTabpage.currentWindow =
                        value?.toJava<IJudoWindow>()
                        ?: throw IllegalArgumentException()
                }
                "buffer" -> throw UnsupportedOperationException("TODO change buffer in Window")

                else -> super.__findattr_ex__(name)
            }
        }
    }

    val scrollable = Py.java2py(judo as IJudoScrollable)

    return object : PyObject() {
        override fun __findattr_ex__(name: String?): PyObject? =
            when (name ?: "") {
                "mapper" -> Py.java2py(judo.mapper)
                "current" -> currentObjects

                "scrollLines",
                "scrollPages",
                "scrollBySetting",
                "scrollToBottom" -> scrollable.__findattr_ex__(name)

                else -> super.__findattr_ex__(name)
            }
    }
}

internal fun createPyTabpage(tabpage: IJudoTabpage) = object : PyObject() {
    override fun __findattr_ex__(name: String?): PyObject? =
        when (name ?: "") {
            "height" -> Py.java2py(tabpage.height)
            "width" -> Py.java2py(tabpage.width)
            "id" -> Py.java2py(tabpage.id)

            else -> super.__findattr_ex__(name)
        }

    override fun __tojava__(c: Class<*>?): Any {
        if (c === IJudoTabpage::class.java) {
            return tabpage
        }
        return super.__tojava__(c)
    }
}

internal fun createPyWindow(
    engine: JythonScriptingEngine,
    tabpage: IJudoTabpage,
    window: IJudoWindow
): PyObject {
    val resize = asPyFn<Int, Unit>("resize", 2) {
        window.resize(it[0], it[1])
    }

    return object : PyObject() {
        override fun __findattr_ex__(name: String?): PyObject? =
            when (name ?: "") {
                "buffer" -> createPyBuffer(window, window.currentBuffer) // cache?
                "height" -> Py.java2py(window.visibleHeight)
                "width" -> Py.java2py(window.width)
                "id" -> Py.java2py(window.id)

                "onSubmit" -> Py.java2py(window.onSubmitFn)

                // not used oft enough to cache
                "close" -> asPyFn<Any, Unit>("close") {
                    tabpage.close(window)
                }
                "resize" -> resize

                else -> super.__findattr_ex__(name)
            }

        override fun __setattr__(name: String?, value: PyObject?) {
            @Suppress("UNCHECKED_CAST")
            when (name) {
                "onSubmit" -> {
                    window.onSubmitFn = when (value) {
                        null -> null
                        is PyNone -> null
                        else -> engine.callableToFunction1(value) as (String) -> Unit
                    }
                }

                else -> super.__setattr__(name, value)
            }
        }

        override fun __tojava__(c: Class<*>?): Any {
            if (c === IJudoWindow::class.java) {
                return window
            }
            return super.__tojava__(c)
        }
    }
}

internal fun createPyBuffer(
    window: IJudoWindow,
    buffer: IJudoBuffer
): PyObject {
    val jsrBase = Jsr223Buffer(window, buffer)
    val base = Py.java2py(jsrBase)
    return JythonBuffer(buffer, jsrBase, base)
}

private class JythonBuffer(
    private val buffer: IJudoBuffer,
    private val jsrBase: Jsr223Buffer,
    private val base: PyObject
) : PyObject() {
    override fun __len__(): Int = buffer.size
    override fun __getitem__(key: Int): PyObject = Py.java2py(jsrBase.get(key))
    override fun __getitem__(key: PyObject?): PyObject =
        if (key is PyInteger) this.__getitem__(key.value)
        else super.__getitem__(key)

    override fun __findattr_ex__(name: String?): PyObject =
        base.__findattr_ex__(name)

    override fun __tojava__(c: Class<*>?): Any {
        if (c === IJudoBuffer::class.java || c === Object::class.java) {
            return buffer
        }
        return super.__tojava__(c)
    }
}

/**
 * If we can't use the Python pattern as a Java pattern,
 *  we have to fall back to this
 */
internal class PyPatternSpec(
    private val pattern: PatternObject,
    override val flags: EnumSet<PatternProcessingFlags>
) : PatternSpec {

    override fun matcher(input: CharSequence): PatternMatcher =
        PyPatternMatcher(
            pattern.groups,
            pattern.finditer(arrayOf(Py.java2py(input.toString())), emptyArray())
                as PyCallIter
        )

    override val original: String = pattern.pattern.string
}

internal class PyPatternMatcher(
    override val groups: Int,
    finditer: PyIterator
) : PatternMatcher {

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

    private fun findInterfaceFor(obj: Any?): Class<*>? {
        if (obj == null) return null
        return exposedInterfaces.firstOrNull {
            it.isInstance(obj)
        }
    }
}

private inline fun <reified T> PyObject.toJava(): T =
    this.__tojava__(T::class.java) as T
