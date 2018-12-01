package net.dhleong.judo.script

import net.dhleong.judo.alias.AliasProcesser
import net.dhleong.judo.alias.compileSimplePatternSpec
import net.dhleong.judo.util.PatternSpec
import java.io.InputStream
import javax.script.Bindings
import javax.script.Invocable
import javax.script.ScriptEngineManager

/**
 * @author dhleong
 */
class Jsr223ScriptingEngine(
    extension: String = "js" // built in to JVM
) : ScriptingEngine {

    open class Factory(
        private val forExtension: String = "js" // built in to JVM
    ) : ScriptingEngine.Factory {

        override val supportsDecorators: Boolean
            get() = forExtension == "py"

        override fun supportsFileType(ext: String): Boolean = ext == forExtension

        override fun create(): ScriptingEngine = Jsr223ScriptingEngine(forExtension)
    }

    private val engine = ScriptEngineManager()
        .getEngineByExtension(extension).also {
            if (it !is Invocable) {
                throw IllegalArgumentException(
                    "JSR223 ScriptEngine $it does not support Invocable"
                )
            }
        }

    override fun register(entity: JudoScriptingEntity) {
        when (entity) {
            is JudoScriptingEntity.Constant<*> -> {
                engine.put(entity.name, entity.value)
            }

            is JudoScriptingEntity.Function<*> -> {
                engine.put(entity.name, entity.toFunctionalInterface(this))
            }
        }
    }

    override fun execute(code: String) {
        engine.eval(code)
    }

    override fun readFile(fileName: String, stream: InputStream) {
        engine.eval(stream.bufferedReader())
    }

    override fun callableToAliasProcessor(fromScript: Any): AliasProcesser {
        return { args ->
            (engine as Invocable).invokeMethod(
                fromScript, "call", *args
            ) as String? ?: ""
        }
    }

    override fun compilePatternSpec(fromScript: Any, flags: String): PatternSpec {
        val flagsSet = patternSpecFlagsToEnums(flags)
        return when (fromScript) {
            is String -> compileSimplePatternSpec(fromScript, flagsSet)
            is Bindings -> {
//                // first, try to compile it as a Java regex Pattern;
//                // that will be much more efficient than delegating
//                // to Python regex stuff (since we have to allocate
//                // arrays for just about every call)
//                val patternAsString = input.pattern.string
//                try {
//                    val javaPattern = Pattern.compile(patternAsString)
//
//                    // if we got here, huzzah! no compile issues
//                    CoercedRegexAliasSpec(
//                        patternAsString,
//                        javaPattern,
//                        input.groups,
//                        flagsSet
//                    )
//                } catch (e: PatternSyntaxException) {
//                    // alas, fallback to using the python pattern
//                    PyPatternSpec(input, flagsSet)
//                }

                throw UnsupportedOperationException("TODO: Regex support: $fromScript / ${fromScript.javaClass}")
            }

            else -> throw IllegalArgumentException(
                "Invalid alias type: $fromScript (${fromScript.javaClass})")
        }
    }
}

