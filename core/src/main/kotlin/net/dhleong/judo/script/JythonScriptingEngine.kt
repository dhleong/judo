package net.dhleong.judo.script

import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.alias.compileSimplePatternSpec
import net.dhleong.judo.modes.InterfaceAdapter
import net.dhleong.judo.modes.PyPatternSpec
import net.dhleong.judo.modes.ScriptExecutionException
import net.dhleong.judo.util.PatternSpec
import org.python.core.Py
import org.python.core.PyException
import org.python.core.PyFunction
import org.python.core.PyObject
import org.python.core.PyStringMap
import org.python.modules.sre.PatternObject
import org.python.util.PythonInterpreter
import java.io.InputStream
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * @author dhleong
 */
class JythonScriptingEngine : ScriptingEngine {
    class Factory : ScriptingEngine.Factory {
        override val supportsDecorators: Boolean
            get() = true

        override fun supportsFileType(ext: String): Boolean = ext == "py"

        override fun create(): ScriptingEngine = JythonScriptingEngine()
    }

    private val python = PythonInterpreter()
    private val keepModules = HashSet<String>()
    private val globals = PyGlobals()

    init {
        InterfaceAdapter.init()

        // the naming here is insane, but correct
        python.locals = globals
    }

    override fun register(entity: JudoScriptingEntity) {
        when (entity) {
            is JudoScriptingEntity.Constant<*> -> {
                globals[entity.name] = entity.value!!
            }

            is JudoScriptingEntity.Function<*> -> {
                globals[entity.name] = entity.toPyFn()
            }
        }
    }

    override fun execute(code: String) {
        wrapExceptions(lineExecution = true) {
            python.exec(code)
        }
    }


//    override fun readFile(file: File, inputStream: InputStream) {
//        file.parentFile?.let { fileDir ->
//            python.exec(
//                """
//                import sys
//                sys.path.insert(0, '${fileDir.absolutePath}')
//                """.trimIndent())
//        }
//        super.readFile(file, inputStream)
//    }

    override fun readFile(fileName: String, stream: InputStream) {
        wrapExceptions {
            python.execfile(stream, fileName)
        }
    }

    override fun callableToAliasProcessor(fromScript: Any): AliasProcesser {
        val handler = fromScript as? PyFunction ?: throw IllegalArgumentException()
        return { args ->
            wrapExceptions {
                handler.__call__(args.map { Py.java2py(it) }.toTypedArray())
                    .__tojava__(String::class.java)
                    as String?
                    ?: ""
            }
        }
    }

    override fun callableToFunction0(fromScript: Any): () -> Unit = {
        wrapExceptions {
            (fromScript as PyFunction).__call__()
        }
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

    private fun <R> JudoScriptingEntity.Function<R>.toPyFn(): PyObject {
        val usages = doc.invocations ?: throw IllegalArgumentException("No invocations for Fn?")
        val hasVarArgs = usages.any { it.hasVarArgs }
        val hasDecorator = usages.any { it.canBeDecorator }
        val maxArgs = when {
            hasVarArgs -> Int.MAX_VALUE
            else -> usages.maxBy { it.args.size }!!.args.size
        }
        val minArgs = usages.minBy { it.args.size }!!.args.size
        val callable = this.toFunctionalInterface(this@JythonScriptingEngine)

        return if (hasDecorator) {
            // TODO does it accept flags?
            asMaybeDecorator<Any>(
                takeArgs = maxArgs,
                minArgs = minArgs
            ) { args -> callable.call(*args) }
        } else {
            asPyFn<Any, Any>(
                takeArgs = maxArgs,
                minArgs = minArgs
            ) { args -> callable.call(*args) }
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

private inline fun <reified T: Any> asMaybeDecorator(
    takeArgs: Int,
    minArgs: Int = takeArgs - 1,
    crossinline fn: (Array<T>) -> Unit
): PyObject = asMaybeDecorator(
    takeArgs, minArgs,
    acceptsFlag = false,
    isFlag = { false },
    fn = fn
)

private inline fun <reified T: Any> asMaybeDecorator(
    takeArgs: Int,
    minArgs: Int = takeArgs - 1,
    crossinline isFlag: (String) -> Boolean,
    crossinline fn: (Array<T>) -> Unit
): PyObject = asMaybeDecorator(
    takeArgs, minArgs,
    acceptsFlag = true,
    isFlag = isFlag,
    fn = fn
)

/**
 * Create a Python function that can be used either as a normal
 * function OR a decorator
 */
private inline fun <reified T: Any> asMaybeDecorator(
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


private inline fun <reified T: Any, reified R> asPyFn(
    takeArgs: Int = 0,
    minArgs: Int = takeArgs,
    crossinline fn: (Array<T>) -> R): PyObject {
    return object : PyObject() {
        override fun __call__(args: Array<PyObject>, keywords: Array<String>): PyObject {
            if (minArgs != Int.MAX_VALUE && args.size < minArgs) {
                throw IllegalArgumentException("Expected $minArgs arguments; got ${args.size}")
            }

            if (args.size > takeArgs) {
                throw IllegalArgumentException("Expected no more than $takeArgs arguments; got ${args.size}")
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
