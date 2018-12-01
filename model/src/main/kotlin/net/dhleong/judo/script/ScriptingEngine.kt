package net.dhleong.judo.script

import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.util.PatternProcessingFlags
import net.dhleong.judo.util.PatternSpec
import java.io.InputStream
import java.util.EnumSet

/**
 * @author dhleong
 */
interface ScriptingEngine {
    interface Factory {
        val supportsDecorators: Boolean

        fun supportsFileType(ext: String): Boolean
        fun create(): ScriptingEngine

        // NOTE all supported types currently use C-style fn calls,
        // so it's a safe default
        fun formatFnCall(name: String, vararg args: String) =
            "$name(${args.joinToString(",")})"
    }

    fun register(entity: JudoScriptingEntity)

    fun <T> registerConst(name: String, doc: JudoScriptDoc, value: T) =
        register(JudoScriptingEntity.Constant(name, doc, value))

    fun <R> registerFn(name: String, doc: JudoScriptDoc, fn: kotlin.Function<R>) =
        register(JudoScriptingEntity.Function(name, doc, fn))

    fun execute(code: String)
    fun readFile(fileName: String, stream: InputStream)

    fun callableToAliasProcessor(fromScript: Any): AliasProcesser
    fun compilePatternSpec(fromScript: Any, flags: String): PatternSpec

    fun patternSpecFlagsToEnums(flags: String) =
        if (flags.isNotBlank()) {
            EnumSet.copyOf(PatternProcessingFlags.NONE).also { flagsSet ->
                if (flags.contains("color", ignoreCase = true)) {
                    flagsSet.add(PatternProcessingFlags.KEEP_COLOR)
                }
            }
        } else {
            PatternProcessingFlags.NONE
        }

    /** Coerce a script-engine type to Java */
    fun toJava(fromScript: Any): Any = fromScript

    /** Coerce a Java type to the Script engine */
    fun toScript(fromJava: Any): Any = fromJava
    fun toScript(fromJavaArray: Array<Any>): Array<Any> =
        Array(fromJavaArray.size) { i ->
            toScript(fromJavaArray[i])
        }
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
