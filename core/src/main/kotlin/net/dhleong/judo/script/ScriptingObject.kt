package net.dhleong.judo.script

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type

/**
 */
annotation class Doc(val body: String)

/**
 * Marker interface for objects that contain methods to be
 * exposed to Scripting
 *
 * @author dhleong
 */
interface ScriptingObject

fun ScriptingObject.extractFunctions() = DocumentedScriptFunction.extractFrom(this.javaClass)

class DocumentedScriptFunction(
    val name: String,
    val doc: JudoScriptDoc,
    internal val methods: List<Method>
) {
    companion object {
        fun extractFrom(javaClass: Class<out ScriptingObject>) =
            javaClass.declaredMethods
                .filter { (it.modifiers and Modifier.PUBLIC) != 0 }
                .filter { "access$" !in it.name }
                .groupBy { it.name }
                .map { (fnName, methods) ->
                    if ("\$default" in fnName) {
                        throw UnsupportedOperationException("Kotlin default parameters are unsupported")
                    }

                    val sortedMethods = methods.sortedBy { it.parameterCount }
                    val doc = sortedMethods.extractJudoDoc()
                    DocumentedScriptFunction(fnName, doc, sortedMethods)
                }
    }
}

private fun List<Method>.extractJudoDoc() = doc {
    val docBody = StringBuilder()
    for (m in this@extractJudoDoc) {
        usage {
            if (m.parameterCount == 1 && m.isVarArgs) {
                withVarArgs()
            } else {
                for (arg in m.parameters) {
                    arg(arg.name, arg.type.stringified())
                }
            }

            if (m.returnType != null && m.returnType?.simpleName != "void") {
                returns(m.returnType.stringified())
            }
        }

        val doc = m.getAnnotation(Doc::class.java)
        if (doc != null) {
            if (docBody.isNotEmpty()) docBody.append("\n\n")
            docBody.append(doc.body.trimIndent())
        }
    }

    if (docBody.isEmpty()) throw IllegalStateException(
        "No documentation provided for ${first().name}"
    )

    body { docBody.toString() }
}

private fun Type.stringified() = when (this) {
    is Class<*> -> this.simpleName
    else -> toString()
}

fun ScriptInitContext.registerFrom(obj: ScriptingObject) {
    DocumentedScriptFunction.extractFrom(obj.javaClass)
        .forEach { f ->
            register(obj, f)
        }
}

private fun ScriptInitContext.register(obj: ScriptingObject, f: DocumentedScriptFunction) {
    if (f.methods.size == 1) {
        val m = f.methods.first()
        if (registerSingle(obj, f, m)) {
            return
        }
    }

    val methodByParamCount = List(f.methods.last().parameterCount + 1) { index ->
        f.methods.firstOrNull { it.parameterCount == index }
    }

    registerFn<Any>(f.name, f.doc) { args: Array<Any> ->
        val firstMethod = methodByParamCount.first { it != null }
        if (firstMethod!!.isVarArgs) {
            return@registerFn firstMethod.invoke(obj, args)
        }

        if (args.size >= methodByParamCount.size) {
            throw IllegalArgumentException("Unexpected number of arguments (${args.size})")
        }
        val m = methodByParamCount[args.size] ?: throw IllegalArgumentException(
            "No overload of ${f.name} accepts ${args.size} arguments"
        )

        m.invoke(obj, *args)
    }
}

private fun ScriptInitContext.registerSingle(
    obj: ScriptingObject,
    f: DocumentedScriptFunction,
    m: Method
): Boolean {
    if (m.isVarArgs) return false

    when (m.parameterCount) {
        0 -> registerFn<Any>(f.name, f.doc) { m.invoke(obj) }
        1 -> registerFn<Any>(f.name, f.doc) { arg: Any ->
            m.invoke(obj, arg)
        }
        2 -> registerFn<Any>(f.name, f.doc) { arg0: Any, arg1: Any ->
            m.invoke(obj, arg0, arg1)
        }

        else -> return false
    }

    return true
}
