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
    val flags: Class<out Enum<*>>? = null
)

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
        args += JudoScriptArgument(name, type, isOptional)
    }

    fun returns(type: String) {
        returnType = type
    }

    fun withVarArgs() {
        hasVarArgs = true
    }

    fun create(): JudoScriptInvocation = JudoScriptInvocation(
        args,
        returnType = returnType,
        canBeDecorator = this.canBeDecorator,
        hasVarArgs = this.hasVarArgs
    )
}

class DocBuilder {
    private var body: String? = null
    private val invocations = mutableListOf<JudoScriptInvocation>()

    inline fun body(block: () -> String) {
        setBody(block())
    }

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
