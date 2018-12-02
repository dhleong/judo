package net.dhleong.judo.script

import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.render.IJudoTabpage
import net.dhleong.judo.render.IJudoWindow
import net.dhleong.judo.util.PatternProcessingFlags
import net.dhleong.judo.util.PatternSpec
import java.io.File
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

    /**
     * When CmdMode is initializing, it calls [register] with every
     * exposed entity to allow this Engine to use them. Before doing so,
     * in case the Engine needs the hints, it will call [onPreRegister];
     * when it's done, it will call [onPostRegister].
     */
    fun onPreRegister() {}
    /** @see [onPreRegister] */
    fun onPostRegister() {}

    fun execute(code: String)
    fun readFile(fileName: String, stream: InputStream)

    fun callableToAliasProcessor(fromScript: Any): AliasProcesser
    fun <R> callableToFunction0(fromScript: Any): Function0<R>
    fun callableToFunction1(fromScript: Any): Function1<Any, Any?>
    fun callableToFunctionN(fromScript: Any): Function1<Array<Any>, Any?>

    /**
     * Return the number of arguments the given callable expects
     */
    fun callableArgsCount(fromScript: Any): Int

    fun compilePatternSpec(fromScript: Any, flags: String): PatternSpec

    fun patternSpecFlagsToEnums(flags: String): EnumSet<PatternProcessingFlags> =
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
    fun toJava(fromScriptArray: Array<Any>): Array<Any> =
        Array(fromScriptArray.size) { i ->
            toJava(fromScriptArray[i])
        }

    /** Coerce a Java type to the Script engine */
    fun toScript(fromJava: Any): Any = fromJava
    fun toScript(fromJavaArray: Array<Any>): Array<Any> =
        Array(fromJavaArray.size) { i ->
            toScript(fromJavaArray[i])
        }

    fun wrapWindow(tabpage: IJudoTabpage, window: IJudoWindow): Any

    /** Called just before a script file is reloaded */
    fun onPreReload() {}

    /** Called just after a script file is reloaded */
    fun onPostReload() {}

    /** Called just before the given file is read */
    fun onPreReadFile(file: File, inputStream: InputStream) {}
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
        val hasMultipleArities: Boolean
            get() {
                var minArity = Int.MAX_VALUE
                var maxArity = Int.MIN_VALUE
                val anyVarArg = doc.invocations?.any { usage ->
                    minArity = minOf(minArity, usage.args.size)
                    maxArity = maxOf(maxArity, usage.args.size)
                    usage.hasVarArgs || usage.args.any { it.isOptional }
                } ?: false

                return anyVarArg || minArity != maxArity
            }
    }
}
