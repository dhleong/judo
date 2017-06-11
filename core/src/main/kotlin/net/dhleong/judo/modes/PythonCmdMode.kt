package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import net.dhleong.judo.complete.CompletionSource
import net.dhleong.judo.input.IInputHistory
import net.dhleong.judo.input.InputBuffer
import org.python.core.Options
import org.python.core.Py
import org.python.core.PyFunction
import org.python.core.PyObject
import org.python.util.PythonInterpreter
import java.io.InputStream

/**
 * Python-based Command mode
 *
 * @author dhleong
 */
class PythonCmdMode(
    judo: IJudoCore,
    inputBuffer: InputBuffer,
    history: IInputHistory,
    private var completions: CompletionSource
) : BaseCmdMode(judo, inputBuffer, history) {

    private val python: PythonInterpreter

    init {
        Options.importSite = false
        python = PythonInterpreter()

        // "constants" (don't know if we can actually make them constant)
        python["MYJUDORC"] = USER_CONFIG_FILE.absolutePath

        // aliasing
        python["alias"] = asMaybeDecorator<Any>(2) {
            defineAlias(it[0] as String, it[1])
        }

        // prompts
        python["prompt"] = asMaybeDecorator<Any>(2) {
            definePrompt(it[0] as String, it[1])
        }

        // triggers
        python["trigger"] = asMaybeDecorator<Any>(2) {
            defineTrigger(it[0] as String, it[1] as PyFunction)
        }

        // map invocations
        python["map"] = asUnitPyFn<Any>(2) { defineMap("", it[0], it[1], true) }
        python["noremap"] = asUnitPyFn<Any>(2) { defineMap("", it[0], it[1], false) }
        python["cmap"] = asUnitPyFn<Any>(2) { defineMap("cmd", it[0], it[1], true) }
        python["cnoremap"] = asUnitPyFn<Any>(2) { defineMap("cmd", it[0], it[1], false) }
        python["imap"] = asUnitPyFn<Any>(2) { defineMap("insert", it[0], it[1], true) }
        python["inoremap"] = asUnitPyFn<Any>(2) { defineMap("insert", it[0], it[1], false) }
        python["nmap"] = asUnitPyFn<Any>(2) { defineMap("normal", it[0], it[1], true) }
        python["nnoremap"] = asUnitPyFn<Any>(2) { defineMap("normal", it[0], it[1], false) }

        python["createMap"] = asUnitPyFn<Any>(4, minArgs = 3) {
            val remap =
                if (it.size == 4) it[3] as Boolean
                else false
            defineMap(it[0] as String, it[1] as String, it[2], remap)
        }

        python["connect"] = asUnitPyFn<Any>(2) { judo.connect(it[0] as String, it[1] as Int) }
        python["createUserMode"] = asUnitPyFn<String>(1) { judo.createUserMode(it[0]) }
        python["disconnect"] = asUnitPyFn<Any> { judo.disconnect() }
        python["echo"] = asUnitPyFn<Any>(Int.MAX_VALUE) { judo.echo(*it) }
        python["enterMode"] = asUnitPyFn<String>(1) { judo.enterMode(it[0]) }
        python["exitMode"] = asUnitPyFn<Any> { judo.exitMode() }
        python["input"] = asPyFn<String, String?>(1, minArgs = 0) {
            if (it.isNotEmpty()) {
                readInput(it[0])
            } else {
                readInput("")
            }
        }
        python["isConnected"] = asPyFn<Any, Boolean> { judo.isConnected() }
        python["load"] = asUnitPyFn<String>(1) { load(it[0]) }
        python["quit"] = asUnitPyFn<Any> { judo.quit() }
        python["reconnect"] = asUnitPyFn<Any> { judo.reconnect() }
        python["reload"] = asUnitPyFn<Any> { reload() }
        python["send"] = asUnitPyFn<String>(1) { judo.send(it[0], true) }
        python["startInsert"] = asUnitPyFn<Any> { judo.enterMode("insert") }
        python["stopInsert"] = asUnitPyFn<Any> { judo.exitMode() }
        python["unalias"] = asUnitPyFn<String>(1) { judo.aliases.undefine(it[0]) }
        python["untrigger"] = asUnitPyFn<String>(1) { judo.triggers.undefine(it[0]) }
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

    private fun readInput(prompt: String): String? {
        // TODO user-provided completions?
        val inputMode = ScriptInputMode(judo, completions, prompt)
        judo.enterMode(inputMode)
        return inputMode.awaitResult()
    }

    override fun execute(code: String) {
        python.exec(code)
    }

    override fun readFile(fileName: String, stream: InputStream) {
        python.execfile(stream, fileName)
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
