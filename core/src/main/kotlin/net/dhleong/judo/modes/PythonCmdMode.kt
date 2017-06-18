package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.JudoRendererInfo
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import org.python.core.Options
import org.python.core.Py
import org.python.core.PyException
import org.python.core.PyFunction
import org.python.core.PyObject
import org.python.core.PyStringMap
import org.python.util.PythonInterpreter
import java.io.File
import java.io.InputStream

/**
 * Python-based Command mode
 *
 * @author dhleong
 */
class PythonCmdMode(
    judo: IJudoCore,
    inputBuffer: InputBuffer,
    rendererInfo: JudoRendererInfo,
    history: IInputHistory,
    private var completions: CompletionSource
) : BaseCmdMode(judo, inputBuffer, rendererInfo, history) {

    private val python: PythonInterpreter

    init {
        Options.importSite = false
        python = PythonInterpreter()

        val globals = PyGlobals()

        // "constants" (don't know if we can actually make them constant)
        globals["MYJUDORC"] = USER_CONFIG_FILE.absolutePath

        // aliasing
        globals["alias"] = asMaybeDecorator<Any>(2) {
            defineAlias(it[0] as String, it[1])
        }

        // prompts
        globals["prompt"] = asMaybeDecorator<Any>(2) {
            definePrompt(it[0] as String, it[1])
        }

        // triggers
        globals["trigger"] = asMaybeDecorator<Any>(2) {
            defineTrigger(it[0] as String, it[1] as PyFunction)
        }

        // map invocations
        sequenceOf(
            "" to "",
            "c" to "cmd",
            "i" to "insert",
            "n" to "normal"
        ).forEach { (letter, modeName) ->
            globals["${letter}map"] = asUnitPyFn<Any>(2) {
                defineMap(modeName, it[0], it[1], true)
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

        globals["connect"] = asUnitPyFn<Any>(2) { judo.connect(it[0] as String, it[1] as Int) }
        globals["complete"] = asUnitPyFn<String>(1) { judo.seedCompletion(it[0]) }
        globals["createUserMode"] = asUnitPyFn<String>(1) { judo.createUserMode(it[0]) }
        globals["disconnect"] = asUnitPyFn<Any> { judo.disconnect() }
        globals["echo"] = asUnitPyFn<Any>(Int.MAX_VALUE) { judo.echo(*it) }
        globals["enterMode"] = asUnitPyFn<String>(1) { judo.enterMode(it[0]) }
        globals["exitMode"] = asUnitPyFn<Any> { judo.exitMode() }
        globals["input"] = asPyFn<String, String?>(1, minArgs = 0) {
            if (it.isNotEmpty()) {
                readInput(it[0])
            } else {
                readInput("")
            }
        }
        globals["isConnected"] = asPyFn<Any, Boolean> { judo.isConnected() }
        globals["load"] = asUnitPyFn<String>(1) { load(it[0]) }
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
        globals["untrigger"] = asUnitPyFn<String>(1) { judo.triggers.undefine(it[0]) }

        // the naming here is insane, but correct
        python.locals = globals
    }

    private fun defineAlias(alias: String, handler: Any) {
        if (handler is PyFunction) {
            judo.aliases.define(alias, { args ->
                handler.__call__(args.map { Py.java2py(it) }.toTypedArray())
                       .__tojava__(String::class.java)
                    as String
            })
        } else {
            judo.aliases.define(alias, handler as String)
        }
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
                { mapTo.__call__() }
            )
        } else {
            throw IllegalArgumentException("Unexpected map-to value")
        }
    }

    private fun definePrompt(alias: String, handler: Any) {
        if (handler is PyFunction) {
            judo.prompts.define(alias, { args ->
                handler.__call__(args.map { Py.java2py(it) }.toTypedArray())
                    .__tojava__(String::class.java)
                    as String
            })
        } else {
            judo.prompts.define(alias, handler as String)
        }
    }

    private fun defineTrigger(alias: String, handler: PyFunction) {
        judo.triggers.define(alias, { args ->
            handler.__call__(args.map { Py.java2py(it) }.toTypedArray())
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

    override fun readFile(file: File) {
        val fileDir = file.parentFile
        python.exec(
            """import sys
              |sys.path.insert(0, '${fileDir.absolutePath}')
            """.trimMargin())
        super.readFile(file)
    }

    override fun readFile(fileName: String, stream: InputStream) {
        wrapExceptions {
            python.execfile(stream, fileName)
        }
    }

    private inline fun wrapExceptions(lineExecution: Boolean = false, block: () -> Unit) {
        try {
            block()
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

/**
 * Create a Python function that can be used either as a normal
 * function OR a decorator
 */
inline private fun <reified T: Any> asMaybeDecorator(
        takeArgs: Int,
        crossinline fn: (Array<T>) -> Unit): PyObject {
    return asPyFn<T, PyObject?>(takeArgs, minArgs = takeArgs-1) { args ->
        if (args.size == takeArgs - 1) {
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
                if (takeArgs == 0) emptyArray<T>()
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
