package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
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
    history: IInputHistory
) : BaseCmdMode(judo, inputBuffer, history) {

    private val python: PythonInterpreter

    init {
        Options.importSite = false
        python = PythonInterpreter()

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
        python["map"] = asUnitPyFn<String>(2) { judo.map("", it[0], it[1], true) }
        python["noremap"] = asUnitPyFn<String>(2) { judo.map("", it[0], it[1], false) }
        python["cmap"] = asUnitPyFn<String>(2) { judo.map("cmd", it[0], it[1], true) }
        python["cnoremap"] = asUnitPyFn<String>(2) { judo.map("cmd", it[0], it[1], false) }
        python["imap"] = asUnitPyFn<String>(2) { judo.map("insert", it[0], it[1], true) }
        python["inoremap"] = asUnitPyFn<String>(2) { judo.map("insert", it[0], it[1], false) }
        python["nmap"] = asUnitPyFn<String>(2) { judo.map("normal", it[0], it[1], true) }
        python["nnoremap"] = asUnitPyFn<String>(2) { judo.map("normal", it[0], it[1], false) }

        python["createmap"] = asUnitPyFn<Any>(4) {
            judo.map(
                it[0] as String,
                it[1] as String,
                it[2] as String,
                it[3] as Boolean)
        }

        python["connect"] = asUnitPyFn<Any> { judo.connect(it[0] as String, it[1] as Int) }
        python["disconnect"] = asUnitPyFn<Any> { judo.disconnect() }
        python["echo"] = asUnitPyFn<Any> { judo.echo(*it) }
        python["quit"] = asUnitPyFn<Any> { judo.quit() }
        python["send"] = asUnitPyFn<String>(1) { judo.send(it[0], true) }
        python["startInsert"] = asUnitPyFn<Any> { judo.enterMode("insert") }
        python["stopInsert"] = asUnitPyFn<Any> { judo.exitMode() }
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
        takeArgs: Int = Int.MAX_VALUE,
        crossinline fn: (Array<T>) -> Unit): PyObject {
    return asPyFn(takeArgs, takeArgs, fn)
}

inline private fun <reified T: Any, reified R> asPyFn(
        takeArgs: Int = Int.MAX_VALUE,
        minArgs: Int = takeArgs,
        crossinline fn: (Array<T>) -> R): PyObject {
    return object : PyObject() {
        override fun __call__(args: Array<PyObject>, keywords: Array<String>): PyObject {
            if (minArgs != Int.MAX_VALUE && args.size < minArgs) {
                throw IllegalArgumentException("Expected $minArgs arguments; got ${args.size}")
            }

            val typedArgs =
                args.take(takeArgs)
                    .map<PyObject, T> { T::class.java.cast(it.__tojava__(T::class.java)) }
                    .toTypedArray()
            val result = fn(typedArgs)
            if (T::class == Unit::class) {
                return Py.None
            }

            return Py.java2py(result)
        }
    }
}
