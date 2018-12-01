package net.dhleong.judo.script

import net.dhleong.judo.alias.compileSimplePatternSpec
import net.dhleong.judo.util.PatternSpec
import java.io.InputStream
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.script.Bindings
import javax.script.Invocable
import javax.script.ScriptEngineManager

/**
 * @author dhleong
 */
abstract class Jsr223ScriptingEngine(
    extension: String = "js" // built in to JVM
) : ScriptingEngine {

    abstract class Factory(
        private val forExtension: String = "js" // built in to JVM
    ) : ScriptingEngine.Factory {

        override val supportsDecorators: Boolean
            get() = forExtension == "py"

        override fun supportsFileType(ext: String): Boolean = ext == forExtension
    }

    protected val engine = ScriptEngineManager()
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
                engine.put(entity.name, toScript(entity))
            }
        }
    }

    override fun execute(code: String) {
        engine.eval(code)
    }

    override fun readFile(fileName: String, stream: InputStream) {
        engine.eval(stream.bufferedReader())
    }

    override fun compilePatternSpec(fromScript: Any, flags: String): PatternSpec {
        val flagsSet = patternSpecFlagsToEnums(flags)
        return when (fromScript) {
            is String -> compileSimplePatternSpec(fromScript, flagsSet)
            is Bindings -> {
                // try to compile it as a Java regex Pattern;
                // that will be much more efficient than delegating
                // to Python regex stuff (since we have to allocate
                // arrays for just about every call)
                val patternAsString = fromScript["source"] as? String
                try {
                    val javaPattern = Pattern.compile(patternAsString)

                    // if we got here, huzzah! no compile issues
                    JavaRegexPatternSpec(
                        patternAsString!!,
                        javaPattern,
                        flagsSet
                    )
                } catch (e: PatternSyntaxException) {
                    scriptRegexToPatternSpec(fromScript)
                }

            }

            else -> throw IllegalArgumentException(
                "Invalid alias type: $fromScript (${fromScript.javaClass})")
        }
    }

    override fun toScript(fromJava: Any): Any {
        if (fromJava is JudoScriptingEntity.Function<*>) {
            return fromJava.toFunctionalInterface(this)
        }

        return super.toScript(fromJava)
    }

    protected open fun scriptRegexToPatternSpec(fromScript: Bindings): PatternSpec {
        throw UnsupportedOperationException(
            "Unsupported regex: $fromScript / ${fromScript.javaClass}"
        )
    }

}

