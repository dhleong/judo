package net.dhleong.judo.script

import java.io.InputStream

/**
 * @author dhleong
 */
interface ScriptingEngine {
    interface Factory {
        fun supportsFileType(ext: String): Boolean
        fun create(): ScriptingEngine
    }

    fun register(entity: JudoScriptingEntity)

    fun <T> registerConst(name: String, doc: JudoScriptDoc, value: T) =
        register(JudoScriptingEntity.Constant(name, doc, value))

    fun <R> registerFn(name: String, doc: JudoScriptDoc, fn: kotlin.Function<R>) =
        register(JudoScriptingEntity.Function(name, doc, fn))

    fun execute(code: String)
    fun readFile(fileName: String, stream: InputStream)
}

sealed class JudoScriptingEntity(
    val name: String,
    val doc: JudoScriptDoc
) {
    class Constant<T>(
        name: String,
        doc: JudoScriptDoc,
        val value: T
    ) : JudoScriptingEntity(name, doc)

    class Function<R>(
        name: String,
        doc: JudoScriptDoc,
        val fn: kotlin.Function<R>
    ) : JudoScriptingEntity(name, doc) {
        val hasMultipleUsages: Boolean
            get() = doc.invocations?.size ?: 0 > 1 ||
                doc.invocations?.any {
                    it.hasVarArgs
                } ?: false
    }
}
