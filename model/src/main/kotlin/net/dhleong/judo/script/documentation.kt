package net.dhleong.judo.script

/**
 * @author dhleong
 */
class JudoScriptDoc(
    val invocations: List<JudoScriptInvocation>?,
    val text: String
)

class JudoScriptInvocation(
    val args: List<JudoScriptArgument>,
    val returnType: String? = null,
    val canBeDecorator: Boolean = false,
    val hasVarArgs: Boolean = false
)

class JudoScriptArgument(
    val name: String,
    val type: String,
    val isOptional: Boolean = false,
    val flags: Class<out Enum<*>>? = null,
    val typeClass: Class<*>? = null
) {
    fun typeMatches(obj: Any): Boolean = typeClass?.isAssignableFrom(obj.javaClass)
        ?: throw IllegalStateException("No typeClass for arg `$name`")
}

class InvocationBuilder(
    private val canBeDecorator: Boolean
) {
    private val args = mutableListOf<JudoScriptArgument>()
    private var hasVarArgs: Boolean = false
    private var returnType: String? = null

    fun arg(name: String, type: String, flags: Class<out Enum<*>>) {
        args += JudoScriptArgument(name, type, isOptional = true, flags = flags)
    }

    fun arg(name: String, type: String, isOptional: Boolean = false) {
        if (canBeDecorator && isOptional) {
            throw IllegalArgumentException(
                "Optional argument `$name` to decorator invocation MUST have type class"
            )
        }
        args += JudoScriptArgument(name, type, isOptional)
    }

    fun arg(name: String, typeClass: Class<*>, isOptional: Boolean = false) {
        args += JudoScriptArgument(name, typeClass.displayName, isOptional, typeClass = typeClass)
    }

    fun returns(type: String) {
        returnType = type
    }

    fun withVarArgs() {
        hasVarArgs = true
    }

    fun create(): JudoScriptInvocation {
        if (canBeDecorator) {
            // validate args per rules for decorators (see below)
            args.forEachIndexed { index, arg ->
                // NOTE: verifying that optional args for decorators
                // have a type class is done eagerly, above

                if (arg.flags != null && index != args.lastIndex - 1) {
                    throw IllegalArgumentException(
                        "Flags `${arg.name}` provided not in penultimate position"
                    )
                }
            }
        }

        return JudoScriptInvocation(
            args,
            returnType = returnType,
            canBeDecorator = this.canBeDecorator,
            hasVarArgs = this.hasVarArgs
        )
    }

    private val Class<*>.displayName: String
        get() = when (this) {
            Integer::class.java -> "Int"

            else -> simpleName
        }

}

class DocBuilder {
    private var body: String? = null
    private val invocations = mutableListOf<JudoScriptInvocation>()

    inline fun body(block: () -> String) {
        setBody(block())
    }

    /**
     * NOTE: Rules for decorators:
     * - They may have a single Flag-type arg; it MUST be the last arg
     *   *before* the handler
     * - They may have optional args; they MUST have a specific type distinct
     *   from required args
     */
    inline fun usage(decorator: Boolean = false, block: InvocationBuilder.() -> Unit) {
        addInvocation(
            InvocationBuilder(decorator).apply {
                block()
            }.create()
        )
    }

    fun setBody(body: String) {
        this.body = body
    }

    fun addInvocation(invocation: JudoScriptInvocation) {
        invocations += invocation
    }

    fun create(): JudoScriptDoc = JudoScriptDoc(
        if (invocations.isEmpty()) null
        else invocations,
        body ?: throw IllegalArgumentException("You must provide a body")
    )
}

inline fun doc(build: DocBuilder.() -> Unit): JudoScriptDoc {
    val builder = DocBuilder()
    builder.build()
    return builder.create()
}
