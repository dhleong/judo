package net.dhleong.judo.modes

import net.dhleong.judo.IJudoCore
import org.python.core.Options
import org.python.core.Py
import org.python.core.PyObject
import org.python.util.PythonInterpreter
import java.io.InputStream

/**
 * Python-based Command mode
 *
 * @author dhleong
 */
class PythonCmdMode(judo: IJudoCore) : BaseCmdMode(judo) {

    private val python: PythonInterpreter

    init {
        Options.importSite = false
        python = PythonInterpreter()

        // map invocations
        python["map"] = asUnitPyFn<String>(2) { judo.map("", it[0], it[1], true) }
        python["noremap"] = asUnitPyFn<String>(2) { judo.map("", it[0], it[1], false) }
        python["nmap"] = asUnitPyFn<String>(2) { judo.map("normal", it[0], it[1], true) }
        python["nnoremap"] = asUnitPyFn<String>(2) { judo.map("normal", it[0], it[1], false) }
        python["imap"] = asUnitPyFn<String>(2) { judo.map("insert", it[0], it[1], true) }
        python["inoremap"] = asUnitPyFn<String>(2) { judo.map("insert", it[0], it[1], false) }

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
        python["send"] = asUnitPyFn<String>(1) { judo.send(it[0]) }
    }

    override fun execute(code: String) {
        python.exec(code)
    }

    override fun readFile(fileName: String, stream: InputStream) {
        python.execfile(stream, fileName)
    }
}

inline private fun <reified T: Any> asUnitPyFn(
        takeArgs: Int = Int.MAX_VALUE,
        crossinline fn: (Array<T>) -> Unit): PyObject {
    return asPyFn(takeArgs, fn)
}

inline private fun <reified T: Any, reified R> asPyFn(
        takeArgs: Int = Int.MAX_VALUE,
        crossinline fn: (Array<T>) -> R): PyObject {
    return object : PyObject() {
        override fun __call__(args: Array<PyObject>, keywords: Array<String>): PyObject {
            if (takeArgs != Int.MAX_VALUE && args.size < takeArgs) {
                throw IllegalArgumentException("Expected $takeArgs arguments; got ${args.size}")
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
