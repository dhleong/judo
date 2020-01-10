package net.dhleong.judo.script

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Doc(val body: String)

@Target(AnnotationTarget.FUNCTION)
annotation class SupportsDecorator

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class FnObject

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class LabeledAs(val typeDisplayName: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class PatternObject

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Flags(val type: KClass<out Enum<*>>)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Optional

/**
 * Marker interface for objects that contain methods to be
 * exposed to Scripting
 *
 * @author dhleong
 */
interface ScriptingObject {
    /**
     * If non-null, all methods in this object will be registered
     * with the returned String prefixed onto the method name. This
     * allows for generating multiple similar methods from a single
     * declaration
     */
    val methodPrefix: String? get() = null

    /**
     * As with [methodPrefix], but as a suffix instead
     */
    val methodSuffix: String? get() = null

    /**
     * If provided, every `@{KEY}` for which `KEY` is a key in this
     * map will be replaced with the associated value in every
     * [Doc] text of every method on this object
     */
    val docVariables: Map<String, String>? get() = null
}

fun ScriptingObject.extractConsts() =
    javaClass.extractConsts()

fun ScriptingObject.extractFunctions() =
    javaClass.extractFunctions()

private typealias MethodInvocation = (Array<Any?>) -> Any?

class DocumentedScriptFunction(
    val name: String,
    val doc: JudoScriptDoc,
    internal val methods: List<Method>
) {
    companion object {
        private val IGNORED_METHODS = setOf(
            "getDocVariables",
            "getMethodPrefix",
            "getMethodSuffix"
        )

        internal fun extractFrom(javaClass: Class<out ScriptingObject>) =
            javaClass.declaredMethods
                .filter { it.name !in IGNORED_METHODS }
                .filter { (it.modifiers and Modifier.PUBLIC) != 0 }
                .filter { "access$" !in it.name }
                .groupBy { it.name }
                .mapNotNull { (fnName, methods) ->
                    if ("\$default" in fnName) {
                        throw UnsupportedOperationException("Kotlin default parameters are unsupported")
                    }

                    // if there's no kotlinFn, it's a property
                    methods[0].kotlinFunction ?: return@mapNotNull null

                    val sortedMethods = methods.sortedBy { it.parameterCount }
                    val doc = sortedMethods.extractJudoDoc()
                    DocumentedScriptFunction(fnName, doc, sortedMethods)
                }
    }
}

class DocumentedScriptConst(
    val name: String,
    val doc: JudoScriptDoc,
    val get: (obj: ScriptingObject) -> Any
)

fun Class<out ScriptingObject>.extractFunctions() =
    DocumentedScriptFunction.extractFrom(this)

fun Class<out ScriptingObject>.extractConsts() =
    kotlin.declaredMemberProperties
        .filter { it.visibility == KVisibility.PUBLIC }
        .filter { it.isFinal }
        .map { prop ->
            DocumentedScriptConst(
                name = prop.name,
                doc = doc { body {
                    prop.findAnnotation<Doc>()?.body
                        ?: throw IllegalStateException("No doc on property ${prop.name}")
                } }
            ) { target ->
                prop.getter.call(target)
                    ?: throw IllegalStateException("No value returned for property ${prop.name}")
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

                        arg.isAnnotationPresent(LabeledAs::class.java) ->
                            arg(
                                arg.name,
                                arg.getAnnotation(LabeledAs::class.java).typeDisplayName,
                                arg.type
                            )

                        else -> arg(
                            arg.name, arg.type.stringified(), arg.type,
                            isOptional = arg.isAnnotationPresent(Optional::class.java)
                        )
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
    is Class<*> -> this.simpleName
        .removePrefix("IScript")
        .capitalize()
    else -> toString()
}.let {
    if (isNullable) "$it?"
    else it
}

fun ScriptInitContext.registerFrom(obj: ScriptingObject) {
    obj.extractConsts()
        .forEach { c ->
            registerConst(c.name, c.doc, c.get(obj))
        }

    obj.extractFunctions()
        .forEach { f ->
            register(obj, f)
        }
}

private fun ScriptInitContext.register(obj: ScriptingObject, f: DocumentedScriptFunction) {
    val invocations = f.doc.invocations ?: throw IllegalStateException("No invocations for `${f.name}`?")
    val anyHaveFlags = invocations.any { inv -> inv.args.any { it.flags != null } }
    if (f.methods.size == 1 && !anyHaveFlags) {
        val m = f.methods.first()
        if (registerSingle(obj, f, m)) {
            return
        }
    }

    // NOTE: if this method has invocations with both flags and an optional,
    // we cannot use the distinct arity registration
    val anyHaveOptions = invocations.any { inv -> inv.args.any { it.isOptional && it.flags == null } }
    if (!(anyHaveFlags && anyHaveOptions)) {
        if (registerDistinctArities(obj, f, invocations)) {
            return
        }
    }

    // "slow" registration is the least efficient at dispatch, but it's the
    // most flexible, able to handle same-arity/different-typed overloads
    registerSlow(obj, f, invocations)
}

private fun ScriptInitContext.registerSingle(
    obj: ScriptingObject,
    f: DocumentedScriptFunction,
    m: Method
): Boolean {
    if (m.isVarArgs) return false

    when (m.parameterCount) {
        0 -> registerFn(obj, f) { m.invoke(obj) }
        1 -> registerFn(obj, f) { arg: Any? ->
            m.invoke(obj, arg)
        }
        2 -> registerFn(obj, f) { arg0: Any?, arg1: Any? ->
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

    val callableByParamCount = MutableList<MethodInvocation?>(f.methods.last().parameterCount + 1) { index ->
        val m = f.methods.firstOrNull { it.parameterCount == index }
        m?.let {
            if (m.isVarArgs) {
                { args: Array<Any?> -> m.invoke(obj, args) }
            } else {
                { args: Array<Any?> -> m.invoke(obj, *args) }
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
                base(providedArgs.withEmptyFlags(flagsIndex))
            }
            break
        }
    }

    registerFn(obj, f) { args: Array<Any?> ->
        val firstMethod = callableByParamCount.first { it != null }
        if (firstMethod != null && f.methods.first().isVarArgs) {
            return@registerFn firstMethod(args)
        }

        if (args.size >= callableByParamCount.size) {
            throw IllegalArgumentException("Unexpected number of arguments (${args.size})")
        }
        val m = callableByParamCount[args.size] ?: throw IllegalArgumentException(
            "No overload of ${f.name} accepts ${args.size} arguments"
        )

        // invoke:
        return@registerFn m(args)
    }
    return true
}

private fun Array<Any?>.withEmptyFlags(flagsIndex: Int): Array<Any?> =
    withValueAtIndex(atIndex = flagsIndex, value = "")

private fun Array<Any?>.withValueAtIndex(atIndex: Int, value: Any?): Array<Any?> =
    if (atIndex < 0) this
    // insert the value
    else Array(this.size + 1) { index -> when (index) {
        in 0 until atIndex -> this[index]
        atIndex -> value
        else -> this[index - 1]
    } }


private sealed class Invokable {
    protected abstract val invocation: JudoScriptInvocation
    abstract val arity: Int
    abstract fun invoke(args: Array<Any?>): Any?
    abstract fun matches(args: Array<Any?>): Boolean

    open fun describe(): String = invocation.args.joinToString(",") {
        "${it.name}:${it.typeClass?.name ?: it.type}".let { base ->
            if (it.isOptional) "[$base]"
            else base
        }
    }

    class Exact(
        private val obj: ScriptingObject,
        override val invocation: JudoScriptInvocation,
        private val method: Method
    ) : Invokable() {
        override val arity: Int = invocation.args.size

        override fun matches(args: Array<Any?>): Boolean =
            invocation.typesMatch(args)

        override fun invoke(args: Array<Any?>): Any? =
            method.invoke(obj, *args)
    }

    class WithoutOptional(
        private val obj: ScriptingObject,
        override val invocation: JudoScriptInvocation,
        private val method: Method,
        private val optionalIndex: Int,
        private val defaultValue: Any?
    ) : Invokable() {
        override val arity: Int = invocation.args.size - 1

        override fun describe(): String = "${super.describe()} without #$optionalIndex"

        override fun matches(args: Array<Any?>): Boolean =
            invocation.typesMatch(args,
                ignoreIndex = { i -> i == optionalIndex }
            )

        override fun invoke(args: Array<Any?>): Any? =
            method.invoke(obj, *args.withValueAtIndex(optionalIndex, value = defaultValue))
    }

    class WithoutOptionalOrFlag(
        private val obj: ScriptingObject,
        override val invocation: JudoScriptInvocation,
        private val method: Method,
        private val optionalIndex: Int,
        private val flagsIndex: Int
    ) : Invokable() {
        override val arity: Int = invocation.args.size - 2

        override fun describe(): String = "${super.describe()} without #$optionalIndex or #$flagsIndex"

        override fun matches(args: Array<Any?>): Boolean =
            invocation.typesMatch(args,
                ignoreIndex = { i ->
                    i == optionalIndex
                        || i == flagsIndex
                }
            )

        override fun invoke(args: Array<Any?>): Any? {
            val actualArgs = arrayOfNulls<Any?>(invocation.args.size)
            var argsOffset = 0
            for (i in actualArgs.indices) {
                actualArgs[i] = when (i) {
                    flagsIndex ->  ""
                    optionalIndex -> null
                    else -> args[argsOffset++]
                }
            }

            return method.invoke(obj, *actualArgs)
        }
    }
}

private fun ScriptInitContext.registerSlow(
    obj: ScriptingObject,
    f: DocumentedScriptFunction,
    invocations: List<JudoScriptInvocation>
) {
    val invokables = mutableListOf<Invokable>()

    for ((i, inv) in invocations.withIndex()) {
        val optionalCount = inv.args.filter { it.flags == null && it.isOptional }.count()
        if (optionalCount > 1) throw IllegalStateException("${f.name} has more than one optional param")

        val flagsIndex = inv.args.indexOfFirst { it.flags != null }
        val optionalIndex = inv.args.indexOfFirst {
            it.flags == null
                && it.isOptional
        }

        val m = f.methods[i]
        invokables += Invokable.Exact(obj, inv, m)

        if (flagsIndex != -1) {
            invokables += Invokable.WithoutOptional(obj, inv, m, flagsIndex, defaultValue = "")
        }
        if (optionalIndex != -1) {
            invokables += Invokable.WithoutOptional(obj, inv, m, optionalIndex, defaultValue = null)
        }
        if (optionalIndex != -1 && flagsIndex != -1) {
            invokables += Invokable.WithoutOptionalOrFlag(obj, inv, m, optionalIndex, flagsIndex)
        }
    }

    registerFn(obj, f, forceDispatchAsMultiArity = true) { args: Array<Any?> ->
        var hasMatchingArity = false
        val invokable = invokables.asSequence()
            .filter { it.arity == args.size }
            .firstOrNull { inv ->
                hasMatchingArity = true
                inv.matches(args)
            }

        if (!hasMatchingArity) {
            throw IllegalArgumentException(
                "No overload of ${f.name} accepts ${args.size} arguments"
            )
        }

        if (invokable == null) {
            throw IllegalArgumentException(
                "${f.name} does not accept args with types: ${args.map { it?.javaClass }}"
            )
        }

        try {
            // invoke:
            invokable.invoke(args)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "${args.toList()} does not match ${invokable.describe()}",
                e
            )
        }
    }
}

private inline fun JudoScriptInvocation.typesMatch(
    args: Array<Any?>,
    ignoreIndex: (Int) -> Boolean = { false }
): Boolean {
    var candidateArgIndex = 0
    for (i in this.args.indices) {
        if (ignoreIndex(i)) {
            continue
        }

        val arg = args[candidateArgIndex]
        if (arg == null) {
            ++candidateArgIndex
            continue
        }

        val expectedArg = this.args[i]
        if (expectedArg.flags != null) {
            @Suppress("ControlFlowWithEmptyBody")
            if (arg is String && expectedArg.acceptsFlag(arg)) {
                // arg match!
                ++candidateArgIndex
            } else {
                // the arg here doesn't match the flags, but flags are optional, so
                // try to match against the next param by falling through to continue:
            }

            continue
        }

        val expectedArgType = expectedArg.typeClass
            ?: throw IllegalStateException(
                "No declared type for arg `${expectedArg.name}`; invocation=$this"
            )

        // NOTE: use the kotlin type mirror for instance to easily handle int vs Integer
        if (!expectedArgType.kotlin.isInstance(arg)) {
            return false
        }

        ++candidateArgIndex
    }

    // if all args were accepted then this matches; if candidateArgIndex
    // is < args.size, that means there was at least one candidateArg that
    // we didn't get to, and we got here because a previous candidateArg
    // matched a later invocation.arg type
    return candidateArgIndex == args.size
}

private fun ScriptInitContext.registerFn(
    obj: ScriptingObject,
    f: DocumentedScriptFunction,
    forceDispatchAsMultiArity: Boolean = false,
    fn: Function<Any?>
) {
    val nameBuilder = StringBuilder()
    obj.methodPrefix?.let { nameBuilder.append(it) }
    nameBuilder.append(f.name)
    obj.methodSuffix?.let { nameBuilder.append(it) }

    val name = nameBuilder.toString()
    val doc = obj.docVariables?.let { vars ->
        f.doc.copy(text = vars.entries.fold(f.doc.text) { text, (k, v) ->
            text.replace("@{$k}", v)
        })
    } ?: f.doc

    registerFn(name, doc, forceDispatchAsMultiArity, fn)
}
