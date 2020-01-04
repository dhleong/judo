package net.dhleong.judo.script

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction

@Target(AnnotationTarget.FUNCTION)
annotation class Doc(val body: String)

@Target(AnnotationTarget.FUNCTION)
annotation class SupportsDecorator

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class FnObject

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class PatternObject

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Flags(val type: KClass<out Enum<*>>)

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
        val isDecorator = m.isAnnotationPresent(SupportsDecorator::class.java)
        usage(decorator = isDecorator) {
            if (m.parameterCount == 1 && m.isVarArgs) {
                withVarArgs()
            } else {
                for (arg in m.parameters) {
                    val flagType = arg.getAnnotation(Flags::class.java)?.type?.javaObjectType
                    when {
                        flagType != null ->
                            arg(arg.name, arg.type.stringified(), flags = flagType)

                        arg.isAnnotationPresent(PatternObject::class.java) ->
                            arg(arg.name, "String/Pattern", arg.type)

                        arg.isAnnotationPresent(FnObject::class.java) ->
                            arg(arg.name, "Fn", arg.type)

                        else -> arg(arg.name, arg.type.stringified(), arg.type)
                    }
                }
            }

            if (m.returnType != null && m.returnType?.simpleName != "void") {
                returns(m.returnType.stringified(
                    isNullable = m.kotlinFunction?.returnType?.isMarkedNullable ?: false
                ))
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

private fun Type.stringified(isNullable: Boolean = false) = when (this) {
    is Class<*> -> this.simpleName.capitalize()
    else -> toString()
}.let {
    if (isNullable) "$it?"
    else it
}

fun ScriptInitContext.registerFrom(obj: ScriptingObject) {
    DocumentedScriptFunction.extractFrom(obj.javaClass)
        .forEach { f ->
            register(obj, f)
        }
}

private fun ScriptInitContext.register(obj: ScriptingObject, f: DocumentedScriptFunction) {
    val invocations = f.doc.invocations ?: throw IllegalStateException("No invocations for `${f.name}`?")
    if (f.methods.size == 1 && invocations.none { inv -> inv.args.any { it.flags != null } }) {
        val m = f.methods.first()
        if (registerSingle(obj, f, m)) {
            return
        }
    }

    if (registerDistinctArities(obj, f, invocations)) {
        return
    }

    registerSlow(obj, f, invocations)
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

/**
 * Register a function with more than one overload of overlapping arities
 */
private fun ScriptInitContext.registerDistinctArities(
    obj: ScriptingObject,
    f: DocumentedScriptFunction,
    invocations: List<JudoScriptInvocation>
): Boolean {
    // verify that all arities are distinct
    f.methods.fold(-1) { lastCount, method ->
        val count = method.parameterCount
        if (count == lastCount) {
            return false
        }
        count
    }

    val callableByParamCount = MutableList(f.methods.last().parameterCount + 1) { index ->
        val m = f.methods.firstOrNull { it.parameterCount == index }
        m?.let {
            if (m.isVarArgs) {
                { args: Array<Any> -> m.invoke(obj, args) }
            } else {
                { args: Array<Any> -> m.invoke(obj, *args) }
            }
        }
    }

    // eagerly register an extra invocation for those with flags
    for (m in invocations) {
        val flagsIndex = m.args.indexOfFirst { it.flags != null }
        if (flagsIndex != -1) {
            if (callableByParamCount[m.args.size - 1] != null) {
                // an arity has an optional flags that overlaps with
                // another arity
                return false
            }

            val base = callableByParamCount[m.args.size]!!
            callableByParamCount[m.args.size - 1] = { providedArgs ->
                // insert the empty flags string
                val args = Array(m.args.size) { index -> when (index) {
                    in 0 until flagsIndex -> providedArgs[index]
                    flagsIndex -> ""
                    else -> providedArgs[index - 1]
                } }
                base(args)
            }
            break
        }
    }

    registerFn<Any>(f.name, f.doc) { args: Array<Any> ->
        val firstMethod = callableByParamCount.first { it != null }
        if (firstMethod != null && f.methods.first().isVarArgs) {
            firstMethod(args)
            return@registerFn
        }

        if (args.size >= callableByParamCount.size) {
            throw IllegalArgumentException("Unexpected number of arguments (${args.size})")
        }
        val m = callableByParamCount[args.size] ?: throw IllegalArgumentException(
            "No overload of ${f.name} accepts ${args.size} arguments"
        )

        // invoke:
        m(args)
    }
    return true
}

private class Invokable(
    private val obj: ScriptingObject,
    val arity: Int,
    private val invocation: JudoScriptInvocation,
    private val method: Method
) {
    fun matches(args: Array<Any?>): Boolean {
        if (arity == args.size) {
            for (i in args.indices) {
                val arg = args[i] ?: continue
                if (!invocation.args[i].typeClass!!.isAssignableFrom(arg.javaClass)) {
                    return false
                }
            }
            return true
        }

        // TODO
        return false
    }

    fun invoke(args: Array<Any?>): Any? {
        return method.invoke(obj, *args)
    }
}

private fun ScriptInitContext.registerSlow(
    obj: ScriptingObject,
    f: DocumentedScriptFunction,
    invocations: List<JudoScriptInvocation>
) {
    val invokables = mutableListOf<Invokable>()

    for ((i, inv) in invocations.withIndex()) {
        val minArgs = inv.args.sumBy {
            if (it.isOptional) 0 else 1
        }
        val maxArgs = inv.args.size

        for (argCount in minArgs..maxArgs) {
            invokables += Invokable(
                obj,
                arity = argCount,
                invocation = inv,
                method = f.methods[i]
            )
        }
    }

    registerFn<Any?>(f.name, f.doc, forceDispatchAsMultiArity = true) { args: Array<Any?> ->
        var hasMatchingArgs = false
        val invokable = invokables.asSequence()
            .filter { it.arity == args.size }
            .firstOrNull { inv ->
                hasMatchingArgs = true
                inv.matches(args)
            }

        if (!hasMatchingArgs) {
            throw IllegalArgumentException(
                "No overload of ${f.name} accepts ${args.size} arguments"
            )
        }

        if (invokable == null) {
            throw IllegalArgumentException(
                "${f.name} does not accept args with types: ${args.map { it?.javaClass }}"
            )
        }

        // invoke:
        invokable.invoke(args)
    }
}

